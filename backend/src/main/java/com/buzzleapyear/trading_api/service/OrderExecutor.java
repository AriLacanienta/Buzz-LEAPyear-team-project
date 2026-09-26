package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * OrderExecutor handles atomic execution of BUY and SELL orders
 * Updates holdings and cash in a single transaction
 * 
 * @author Ari Lacanienta
 */
@Service
public class OrderExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderExecutor.class);
    
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;

    public OrderExecutor(AccountRepository accountRepository, HoldingRepository holdingRepository) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
    }

    /**
     * Execute a BUY order atomically
     * - Get or create Holding for the instrument
     * - Update quantity and total cost
     * - Deduct from cashReserved
     * - Save all changes in single transaction
     * 
     * @param account the account making the purchase
     * @param instrument the instrument being purchased
     * @param order the trade order
     */
    public void executeBuyOrder(Account account, Instrument instrument, TradeOrder order) {
        logger.info("Executing BUY order ID: {} for {} shares at ${}", 
            order.getId(), order.getQuantity(), order.getPrice());
        
        // Get or create holding for this instrument
        Holding holding = getOrCreateHolding(account, instrument);

        // Update holding quantity and total cost
        updateHoldingQuantity(holding, order, instrument);


        // Deduct from cash reserved (was already deducted from available on VALIDATED)
        updateCashReserved(account, order);
    }

    private Holding getOrCreateHolding(Account account, Instrument instrument) {
        Optional<Holding> existingHolding = holdingRepository.findByAccountIdAndInstrumentId(
            account.getId(), instrument.getId());
        
        Holding holding = existingHolding.orElseGet(() -> {
            Holding newHolding = new Holding();
            newHolding.setAccount(account);
            newHolding.setInstrument(instrument);
            newHolding.setQuantity(BigDecimal.ZERO);
            newHolding.setTotalCost(BigDecimal.ZERO);
            return newHolding;
        });
        return holding;
    }

    private void updateHoldingQuantity(Holding holding, TradeOrder order, Instrument instrument) {
        BigDecimal orderCost = order.getPrice().multiply(order.getQuantity());
        holding.setQuantity(holding.getQuantity().add(order.getQuantity()));
        holding.setTotalCost(holding.getTotalCost().add(orderCost));
        
        // Save holding (will insert if new, update if existing)
        holdingRepository.save(holding);
        logger.info("Holding updated: {} - quantity now: {}, total cost: {}", 
            instrument.getInstrumentSymbol(), holding.getQuantity(), holding.getTotalCost());
    }

        private void updateCashReserved(Account account, TradeOrder order) {
        BigDecimal orderCost = order.getPrice().multiply(order.getQuantity());
        account.setCashReserved(account.getCashReserved().subtract(orderCost));
        accountRepository.save(account);
        logger.info("Account cash updated - reserved reduced by: ${}. New reserved: ${}", 
            orderCost, account.getCashReserved());
    }

    /**
     * Execute a SELL order atomically
     * - Get Holding for the instrument (must exist)
     * - Update quantity (reduce by order quantity)
     * - Update total cost (proportional reduction)
     * - Add to cashAvailable
     * - Delete Holding if quantity becomes 0
     * - Save all changes in single transaction
     * 
     * @param account the account making the sale
     * @param instrument the instrument being sold
     * @param order the trade order
     */
    public void executeSellOrder(Account account, Instrument instrument, TradeOrder order) {
        logger.info("Executing SELL order ID: {} for {} shares at ${}", 
            order.getId(), order.getQuantity(), order.getPrice());
        
        // Get holding for this instrument (must exist after validation)
        Holding holding = holdingRepository.findByAccountIdAndInstrumentId(
            account.getId(), instrument.getId())
            .orElseThrow(() -> new RuntimeException(
                "Holding not found for account: " + account.getId() + 
                ", instrument: " + instrument.getInstrumentSymbol()));

        // Calculate proportional cost reduction
        // Average cost per unit = total_cost / quantity
        // Cost to reduce = average_cost_per_unit * order_quantity
        BigDecimal averageCostPerUnit = holding.getTotalCost().divide(holding.getQuantity(), 10, RoundingMode.HALF_UP);
        BigDecimal costReduction = averageCostPerUnit.multiply(order.getQuantity());

        // Update holding quantity and total cost
        holding.setQuantity(holding.getQuantity().subtract(order.getQuantity()));
        holding.setTotalCost(holding.getTotalCost().subtract(costReduction));

        // If quantity reaches 0, delete the holding; otherwise save
        if (holding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            holdingRepository.delete(holding);
            logger.info("Holding deleted: {} (quantity reduced to zero)", instrument.getInstrumentSymbol());
        } else {
            holdingRepository.save(holding);
            logger.info("Holding updated: {} - quantity now: {}, total cost: {}", 
                instrument.getInstrumentSymbol(), holding.getQuantity(), holding.getTotalCost());
        }

        // Add proceeds to cash available
        BigDecimal proceeds = order.getPrice().multiply(order.getQuantity());
        account.setCashAvailable(account.getCashAvailable().add(proceeds));
        accountRepository.save(account);
        logger.info("Account cash updated - available increased by: ${}. New available: ${}", 
            proceeds, account.getCashAvailable());
    }
}

