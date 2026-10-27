package com.buzzleapyear.trading_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.buzzleapyear.trading_api.repository.HoldingRepository;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrder.QuantityMode;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.service.QuoteService;
import com.buzzleapyear.trading_api.dto.QuoteResponseDto;
import com.buzzleapyear.trading_api.dto.TradeOrderPreviewResponseDto;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Holding;
import org.springframework.beans.factory.annotation.Value;

/**
 * OrderProcessor handles order preview and processing workflow
 * Orchestrates: price matching -> validation -> execution
 * 
 * @author Ari Lacanienta
 */
@Service
public class OrderProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);
    
    private final OrderValidator orderValidator;
    private final OrderExecutor orderExecutor;
    private final TradeOrderRepository tradeOrderRepository;
    private final TradeOrderStatusRepository tradeOrderStatusRepository;
    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final QuoteService quoteService;
    private final ExchangeRateService exchangeRateService;

    // Maximum allowed drift between quoted price and the live price
    private static final BigDecimal PRICE_TOLERANCE = new BigDecimal("0.01");
    private static final Duration MAX_QUOTE_AGE = Duration.ofSeconds(10);
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Value("${ORDER_EXECUTION_DELAY_MS:1000}")
    private long orderExecutionDelayMs;

    public OrderProcessor(
            OrderValidator orderValidator,
            OrderExecutor orderExecutor,
            TradeOrderRepository tradeOrderRepository,
            TradeOrderStatusRepository tradeOrderStatusRepository,
            HoldingRepository holdingRepository,
            AccountRepository accountRepository,
            QuoteService quoteService,
            ExchangeRateService exchangeRateService) {
        this.orderValidator = orderValidator;
        this.orderExecutor = orderExecutor;
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderStatusRepository = tradeOrderStatusRepository;
        this.holdingRepository = holdingRepository;
        this.accountRepository = accountRepository;
        this.quoteService = quoteService;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Main method to process an order
     * Orchestrates the entire workflow: SUBMITTED -> ACCEPTED -> FILLED/REJECTED
     * 
     * @param orderId the trade order ID to process
     */
    public void processOrder(Long orderId) {
        TradeOrder order = tradeOrderRepository.findByIdForProcessing(orderId).orElse(null);
        if (order == null) {
            logger.error("Order ID: {} could not be loaded for processing", orderId);
            return;
        }

        Account account = order.getAccount();
        BigDecimal acceptedPrice = null;
        boolean cashReserved = false;
        try {
            logger.info("Order ID: {} processing initiated", order.getId());

            // Step 1: Price the order from the live quote service
            QuoteService.QuoteSnapshot acceptanceQuote = getCurrentQuote(order);
            if (acceptanceQuote == null) {
                String reason = "No current market quote available for " + order.getInstrument().getInstrumentSymbol();
                logOrderStatus(order, OrderStatus.REJECTED, reason);
                logger.warn("Order ID: {} rejected: {}", order.getId(), reason);
                return;
            }

            BigDecimal submittedPrice = order.getPrice();
            BigDecimal marketPrice = acceptanceQuote.price();
            if (!isWithinTolerance(order.getPrice(), marketPrice)) {
                String reason = String.format("Price moved: quoted $%.2f, market $%.2f", order.getPrice(), marketPrice);
                logOrderStatus(order, OrderStatus.REJECTED, reason);
                logger.warn("Order ID: {} rejected: {}", order.getId(), reason);
                return;
            }

            order.setPrice(marketPrice);
            if (order.getQuantityMode() == QuantityMode.AMOUNT) {
                BigDecimal resolvedQuantity = resolveQuantity(order.getInstrument(), QuantityMode.AMOUNT, order.getQuantity(),
                    order.getRequestedAmount(), marketPrice, order.getSide(), getHeldQuantity(account, order.getInstrument()));
                if (resolvedQuantity == null || resolvedQuantity.signum() <= 0) {
                    String reason = "Order amount is too small to buy any quantity at the current price";
                    logOrderStatus(order, OrderStatus.REJECTED, reason);
                    logger.warn("Order ID: {} rejected: {}", order.getId(), reason);
                    return;
                }
                order.setQuantity(resolvedQuantity);
            }
            BigDecimal exchangeRate = exchangeRateService.getToUsdRate(order.getInstrument().getCurrencyCode());
            order.setNativeCurrency(order.getInstrument().getCurrencyCode());
            order.setExchangeRate(exchangeRate);
            order.setTradeValue(exchangeRateService.toUsd(
                marketPrice.multiply(order.getQuantity()), order.getNativeCurrency()));
            tradeOrderRepository.save(order);

            // Step 2: Validate order against business rules
            logger.info("Validating order ID: {}", order.getId());
            OrderValidator.ValidationResult validationResult = orderValidator.validate(order, account);
            if (!validationResult.isValid()) {
                logOrderStatus(order, OrderStatus.REJECTED, validationResult.getReason());
                return;
            }

            // Step 3: Reserve buy cash before acceptance
            logger.info("Order ID: {} passed validation", order.getId());

            if (order.getSide() == OrderSide.BUY) {
                BigDecimal requiredCash = order.getTradeValue();
                account.setCashAvailable(account.getCashAvailable().subtract(requiredCash));
                account.setCashReserved(account.getCashReserved().add(requiredCash));
                accountRepository.save(account);
                acceptedPrice = order.getPrice();
                cashReserved = true;
                logger.info("Cash reserved for BUY order ID: {} - Amount: {}", order.getId(), requiredCash);
            }

            if (acceptedPrice == null) {
                acceptedPrice = order.getPrice();
            }
            logOrderStatus(order, OrderStatus.ACCEPTED, null);

        } catch (Exception e) {
            logger.error("Unexpected error accepting order ID: {}", order.getId(), e);
            if (cashReserved) {
                releaseReservedCash(order, account, acceptedPrice);
            }
            logOrderStatus(order, OrderStatus.REJECTED, "System error: " + e.getMessage());
            return;
        }

        if (!waitForAcceptanceVisibility()) {
            rejectAcceptedOrder(order, account, acceptedPrice, "Order Processing interrupted");
            return;
        }
        executeValidOrder(order, account, acceptedPrice);
    }

    public BigDecimal resolveQuantity(Instrument instrument, QuantityMode mode, BigDecimal quantity, BigDecimal amount, BigDecimal price, OrderSide side, BigDecimal heldQuantity) {
        if (mode != QuantityMode.AMOUNT) {
            return quantity;
        }
        if (amount == null || price == null || price.signum() <= 0) {
            return null;
        }
        BigDecimal usdPrice = price.multiply(exchangeRateService.getToUsdRate(instrument.getCurrencyCode()));
        int quantityPrecision = instrument.getQuantityPrecision();
        BigDecimal resolved = amount.divide(usdPrice, quantityPrecision, RoundingMode.DOWN);
        if(side == OrderSide.SELL && heldQuantity != null && heldQuantity.signum() > 0) {
            BigDecimal heldValue = exchangeRateService.toUsd(
                heldQuantity.multiply(price), instrument.getCurrencyCode());
            if (heldValue.subtract(amount).abs().compareTo(ONE_CENT) <= 0) {
                resolved = heldQuantity;
            }
        }
        return resolved.setScale(quantityPrecision, RoundingMode.DOWN);
    }

    private BigDecimal getHeldQuantity(Account account, Instrument instrument) {
        return holdingRepository.findByAccountIdAndInstrumentId(account.getId(), instrument.getId())
            .map(Holding::getQuantity)
            .orElse(BigDecimal.ZERO);
    }

    public TradeOrderPreviewResponseDto previewOrder(Account account, Instrument instrument, OrderSide side, 
            QuantityMode mode, BigDecimal requestedQuantity, BigDecimal requestedAmount) {
        Holding holding = holdingRepository.findByAccountIdAndInstrumentId(account.getId(), instrument.getId()).orElse(null);

        BigDecimal heldQuantity = holding != null ? holding.getQuantity() : BigDecimal.ZERO;
        BigDecimal heldCost = holding != null ? holding.getTotalCost() : BigDecimal.ZERO;
        BigDecimal cashAvailable = account.getCashAvailable();

        BigDecimal livePrice = quoteService.getLatestPrice(instrument.getInstrumentSymbol()).orElse(null);
        if (livePrice == null) {
            return new TradeOrderPreviewResponseDto(
                instrument.getInstrumentSymbol(),
                side,
                mode,
                requestedQuantity,
                requestedAmount,
                null,
                null,
                cashAvailable,
                cashAvailable,
                heldQuantity,
                heldQuantity,
                null,
                null,
                false,
                "No market quote available for " + instrument.getInstrumentSymbol(),
                instrument.getCurrencyCode(),
                null,
                null);
        }

        BigDecimal quantity = resolveQuantity(instrument, mode, requestedQuantity, requestedAmount, livePrice, side, heldQuantity);
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        BigDecimal nativeValue = livePrice.multiply(quantity);
        BigDecimal exchangeRate = exchangeRateService.getToUsdRate(instrument.getCurrencyCode());
        BigDecimal estimatedValue = nativeValue.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        TradeOrder draft = new TradeOrder();
        draft.setAccount(account);
        draft.setInstrument(instrument);
        draft.setSide(side);
        draft.setQuantityMode(mode);
        draft.setRequestedAmount(requestedAmount);
        draft.setQuantity(quantity);
        draft.setPrice(livePrice);
        
        OrderValidator.ValidationResult result = orderValidator.validatePreview(draft, account);

        BigDecimal cashAfter;
        BigDecimal holdingQuantityAfter;
        BigDecimal averageCostAfter = null;
        BigDecimal realizedPnL = null;

        if (side == OrderSide.BUY) {
            cashAfter = cashAvailable.subtract(estimatedValue);
            holdingQuantityAfter = heldQuantity.add(quantity);
            if (holdingQuantityAfter.signum() > 0) {
                averageCostAfter = heldCost.add(estimatedValue).divide(holdingQuantityAfter, 4, RoundingMode.HALF_UP);
            }
        } 
        else {
            cashAfter = cashAvailable.add(estimatedValue);
            holdingQuantityAfter = heldQuantity.subtract(quantity);
            if (heldQuantity.signum() > 0) {
                BigDecimal averageCost = heldCost.divide(heldQuantity, 4, RoundingMode.HALF_UP);
                realizedPnL = estimatedValue.subtract(averageCost.multiply(quantity)).setScale(2, RoundingMode.HALF_UP);
                averageCostAfter = holdingQuantityAfter.signum() > 0 ? averageCost : null;
            }
        }

        return new TradeOrderPreviewResponseDto(
            instrument.getInstrumentSymbol(),
            side,
            mode,
            quantity,
            requestedAmount,
            livePrice,
            estimatedValue,
            cashAvailable,
            cashAfter,
            heldQuantity,
            holdingQuantityAfter,
            averageCostAfter,
            realizedPnL,
            result.isValid(),
            result.getReason(),
            instrument.getCurrencyCode(),
            exchangeRate,
            nativeValue
        );
    }

    private boolean isWithinTolerance(BigDecimal quotedPrice, BigDecimal marketPrice) {
        if (quotedPrice == null || marketPrice == null || marketPrice.signum() <= 0) {
            return false;
        }
        BigDecimal deviation = quotedPrice.subtract(marketPrice).abs().divide(marketPrice, 6, RoundingMode.HALF_UP);
        return deviation.compareTo(PRICE_TOLERANCE) <= 0;
    }

    private QuoteService.QuoteSnapshot getCurrentQuote(TradeOrder order) {
        QuoteService.QuoteSnapshot quote = quoteService.getLatestQuoteSnapshotBySymbol(order.getInstrument().getInstrumentSymbol()).orElse(null);
        if (quote == null || quote.timestamp() == null || quote.timestamp().isBefore(LocalDateTime.now().minus(MAX_QUOTE_AGE))) {
            return null;
        }
        return quote;
    }

    private boolean waitForAcceptanceVisibility() {
        if (orderExecutionDelayMs <= 0) {
            return true;
        }
        try {
            Thread.sleep(orderExecutionDelayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Execute the validated order - updates Account cash, Holdings, and TradeOrderStatus atomically
     * @param order
     * @param account
     * @param marketPrice
     */

    private void executeValidOrder(TradeOrder order, Account account, BigDecimal acceptedPrice) {
        QuoteService.QuoteSnapshot executionQuote;
        try {
            executionQuote = getCurrentQuote(order);
        }
        catch (Exception e) {
            rejectAcceptedOrder(order, account, acceptedPrice, "Could not retrieve a current market quote");
            logger.error("Order ID: {} execution quote lookup failed", order.getId(), e);
            return;
        }
        
        if (executionQuote == null) {
            rejectAcceptedOrder(order, account, acceptedPrice, "No current market quote available at execution");
            return;
        }

        BigDecimal executionPrice = executionQuote.price();
        if (!isWithinTolerance(acceptedPrice, executionPrice)) {
            String reason = String.format("Price moved before execution: accepted $%.2f, market $%.2f", acceptedPrice, executionPrice);
            rejectAcceptedOrder(order, account, acceptedPrice, reason);
            return;
        }

        order.setPrice(executionPrice);
        order.setTradeValue(exchangeRateService.toUsd(
            executionPrice.multiply(order.getQuantity()), order.getNativeCurrency()));
        try {
            tradeOrderRepository.save(order);
            if (order.getSide() == OrderSide.BUY) {
                BigDecimal acceptedUsdValue = exchangeRateService.toUsd(
                    acceptedPrice.multiply(order.getQuantity()), order.getNativeCurrency());
                orderExecutor.executeBuyOrder(account, order.getInstrument(), order, acceptedUsdValue);
            }
            else {
                orderExecutor.executeSellOrder(account, order.getInstrument(), order);
            }
        }
        catch (Exception e) {
            String reason = "Execution failed: " + e.getMessage();
            rejectAcceptedOrder(order, account, acceptedPrice, reason);
            logger.error("Order ID: {} execcution failed", order.getId(), e);
            return;
        }

        logOrderStatus(order, OrderStatus.FILLED, null);
        logger.info("Order ID: {} FILLED successfully at price: {}", order.getId(), order.getPrice());
    }

    /**
     * Handle order rejection - restore cash if BUY order
     * @param order the order to reject
     * @param account the account for the order
     * @param reason the rejection reason
     */
    private void rejectAcceptedOrder(TradeOrder order, Account account, BigDecimal acceptedPrice, String reason) {
        if (order.getSide() == OrderSide.BUY) {
            releaseReservedCash(order, account, acceptedPrice);
        }
        logOrderStatus(order, OrderStatus.REJECTED, reason);
    }

    private void releaseReservedCash(TradeOrder order, Account account, BigDecimal acceptedPrice) {
        Account currentAccount = accountRepository.findById(account.getId()).orElse(account);
        BigDecimal reservedCash = acceptedPrice.multiply(order.getQuantity())
            .multiply(order.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
        currentAccount.setCashReserved(currentAccount.getCashReserved().subtract(reservedCash));
        currentAccount.setCashAvailable(currentAccount.getCashAvailable().add(reservedCash));
        accountRepository.save(currentAccount);
        logger.info("Cash restored for rejected BUY order ID: {} - Amount: {}", order.getId(), reservedCash);
    }   

    /**
     * Get the latest status for an order
     * @param orderId the order ID
     * @return the latest TradeOrderStatus, or null if not found
     */
    private TradeOrderStatus getLatestOrderStatus(Long orderId) {
        return tradeOrderStatusRepository.findAll().stream()
            .filter(status -> status.getTradeOrder().getId().equals(orderId))
            .max((s1, s2) -> s1.getTimeUpdated().compareTo(s2.getTimeUpdated()))
            .orElse(null);
    }

    /**
     * Log a status update for the order
     * @param order the order
     * @param status the new status
     * @param reason the reason for the status (if applicable)
     */
    private void logOrderStatus(TradeOrder order, OrderStatus status, String reason) {
        TradeOrderStatus orderStatus = new TradeOrderStatus();
        orderStatus.setTradeOrder(order);
        orderStatus.setStatus(status);
        orderStatus.setTimeUpdated(LocalDateTime.now());
        orderStatus.setReasonText(reason);
        
        tradeOrderStatusRepository.save(orderStatus);
        logger.debug("Order ID: {} status logged: {} {}", 
            order.getId(), status, reason != null ? "- " + reason : "");
    }
}
