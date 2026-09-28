package com.buzzleapyear.trading_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;

/**
 * OrderProcessor handles order processing workflow
 * Orchestrates: validation -> price matching -> execution
 * 
 * @author Ari Lacanienta
 */
@Service
public class OrderProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);
    
    private final OrderValidator orderValidator;
    private final OrderExecutor orderExecutor;
    private final MarketQuoteService marketQuoteService;
    private final TradeOrderStatusRepository tradeOrderStatusRepository;
    private final AccountRepository accountRepository;

    public OrderProcessor(
            OrderValidator orderValidator,
            OrderExecutor orderExecutor,
            MarketQuoteService marketQuoteService,
            TradeOrderStatusRepository tradeOrderStatusRepository,
            AccountRepository accountRepository) {
        this.orderValidator = orderValidator;
        this.orderExecutor = orderExecutor;
        this.marketQuoteService = marketQuoteService;
        this.tradeOrderStatusRepository = tradeOrderStatusRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Main method to process an order
     * Orchestrates the entire workflow: SUBMITTED -> VALIDATED -> FILLED/REJECTED
     * 
     * @param order the trade order to process
     */
    public void processOrder(TradeOrder order) {
        try {
            logger.info("Order ID: {} processing initiated", order.getId());
                    
            Account account = order.getAccount();

            // Step 1: Validate order against business rules
            logger.info("Validating order ID: {}", order.getId());
            OrderValidator.ValidationResult validationResult = orderValidator.validate(order, account);
            if (!validationResult.isValid()) {
                logOrderStatus(order, OrderStatus.REJECTED, validationResult.getReason());
                return;
            }

            // Step 2: Log VALIDATED status and reserve cash if BUY order
            logger.info("Order ID: {} passed validation", order.getId());
            logOrderStatus(order, OrderStatus.VALIDATED, null);
            
            if (order.getSide() == OrderSide.BUY) {
                BigDecimal requiredCash = order.getPrice().multiply(order.getQuantity());
                account.setCashAvailable(account.getCashAvailable().subtract(requiredCash));
                account.setCashReserved(account.getCashReserved().add(requiredCash));
                accountRepository.save(account);
                logger.info("Cash reserved for BUY order ID: {} - Amount: {}", order.getId(), requiredCash);
            }
            
            TradeOrderStatus latestStatus = tradeOrderStatusRepository.getLatestStatusById(order.getId());
            if (latestStatus != null && latestStatus.getStatus() != OrderStatus.VALIDATED) {
                logger.warn("Order ID: {} status changed from VALIDATED to {}. Aborting execution.", 
                    order.getId(), latestStatus.getStatus());
                return;
            }

            // Step 4: Get market quote and perform price matching
            logger.info("Fetching market quote for instrument: {}", order.getInstrument().getInstrumentSymbol());
            BigDecimal marketPrice = marketQuoteService.getLatestPrice(order.getInstrument());
            
            if (marketPrice == null) {
                String reason = "No market quote available for " + order.getInstrument().getInstrumentSymbol();
                handleOrderRejection(order, account, reason);
                logger.warn("Order ID: {} rejected: {}", order.getId(), reason);
                return;
            }

            // Step 5: Price matching and execution
            executeValidOrder(order, account, marketPrice);
        } catch (Exception e) {
            logger.error("Unexpected error processing order ID: {}", order != null ? order.getId() : "unknown", e);
            if (order != null) {
                logOrderStatus(order, OrderStatus.REJECTED, "System error: " + e.getMessage());
            }
        }
    }

    /**
     * Execute the validated order - updates Account cash, Holdings, and TradeOrderStatus atomically
     * @param refreshedOrder
     * @param account
     * @param marketPrice
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private void executeValidOrder(TradeOrder refreshedOrder, Account account, BigDecimal marketPrice){
        if (refreshedOrder.getPrice().compareTo(marketPrice) == 0) {
            // Price matches - execute the order
            logger.info("Price match confirmed for order ID: {}. Market: {}, Order: {}", 
                refreshedOrder.getId(), marketPrice, refreshedOrder.getPrice());
            
            try {
                if (refreshedOrder.getSide() == OrderSide.BUY) {
                    orderExecutor.executeBuyOrder(account, refreshedOrder.getInstrument(), refreshedOrder);
                } else {
                    orderExecutor.executeSellOrder(account, refreshedOrder.getInstrument(), refreshedOrder);
                }
                
                logOrderStatus(refreshedOrder, OrderStatus.FILLED, null);
                logger.info("Order ID: {} FILLED successfully", refreshedOrder.getId());
            } catch (Exception e) {
                // If execution fails, restore cash and reject
                String reason = "Execution failed: " + e.getMessage();
                handleOrderRejection(refreshedOrder, account, reason);
                logger.error("Order ID: {} execution failed", refreshedOrder.getId(), e);
            }
        } else {
            // Price mismatch - reject the order
            String reason = String.format("Price mismatch: market $%.2f ≠ order $%.2f", 
                marketPrice, refreshedOrder.getPrice());
            handleOrderRejection(refreshedOrder, account, reason);
            logger.warn("Order ID: {} rejected: {}", refreshedOrder.getId(), reason);
        }
    }

    /**
     * Handle order rejection - restore cash if BUY order
     * @param order the order to reject
     * @param account the account for the order
     * @param reason the rejection reason
     */
    private void handleOrderRejection(TradeOrder order, Account account, String reason) {
        // Restore cash if BUY order
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal reservedCash = order.getPrice().multiply(order.getQuantity());
            account.setCashReserved(account.getCashReserved().subtract(reservedCash));
            account.setCashAvailable(account.getCashAvailable().add(reservedCash));
            accountRepository.save(account);
            logger.info("Cash restored for rejected BUY order ID: {} - Amount: {}", order.getId(), reservedCash);
        }
        
        logOrderStatus(order, OrderStatus.REJECTED, reason);
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
