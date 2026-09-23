package com.buzzleapyear.trading_api.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;

/**
 * OrderValidator
 * Validates trade orders against business rules
 * 
 * @author Ari Lacanienta
 */
@Component 
public class OrderValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderValidator.class);

    public OrderValidator(){}

    /**
     * Validate that order quantity is positive
     * @param quantity the quantity to validate
     * @return true if quantity is valid, false otherwise
     */
    public boolean validateOrderQuantity(BigDecimal quantity) {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validate that account has sufficient cash available for a BUY order
     * @param account the account to validate
     * @param requiredCash the amount of cash required
     * @return true if account has sufficient cash, false otherwise
     */
    public boolean validateAccountHasSufficientCash(Account account, BigDecimal requiredCash) {
        if (account == null || account.getCashAvailable() == null || requiredCash == null) {
            return false;
        }
        return account.getCashAvailable().compareTo(requiredCash) >= 0;
    }

    /**
     * Validate that account can sell an instrument (has holdings for it)
     * @param account the account to validate
     * @param instrument the instrument to validate
     * @return true if account has a holding for this instrument, false otherwise
     */
    public boolean validateAccountHasInstrument(Account account, Instrument instrument) {
        if (account == null || account.getHoldings() == null || instrument == null) {
            return false;
        }
        
        return account.getHoldings().stream()
            .anyMatch(holding -> holding.getInstrument().getId().equals(instrument.getId()));
    }

    /**
     * Validate that account has sufficient quantity of an instrument to SELL
     * @param account the account to validate
     * @param instrument the instrument to sell
     * @param quantity the quantity to sell
     * @return true if account has sufficient quantity, false otherwise
     */
    public boolean validateAccountHasSufficientQuantity(Account account, Instrument instrument, BigDecimal quantity) {
        if (account == null || account.getHoldings() == null || instrument == null || quantity == null) {
            return false;
        }
        
        return account.getHoldings().stream()
            .filter(holding -> holding.getInstrument().getId().equals(instrument.getId()))
            .anyMatch(holding -> holding.getQuantity().compareTo(quantity) >= 0);
    }

    /**
     * Validate that the trade respects the account's risk profile limits
     * Risk profile limits per holding:
     * - CONSERVATIVE: $10,000 max
     * - MODERATE: $50,000 max
     * - AGGRESSIVE: no limit
     * 
     * @param account the account to validate
     * @param order the order to validate
     * @return true if trade respects risk profile, false otherwise
     */
    public boolean validateRiskProfile(Account account, TradeOrder order) {
        if (account == null || account.getRiskProfile() == null || order == null) {
            return false;
        }
        
        BigDecimal holdingCost = order.getPrice().multiply(order.getQuantity());
        
        switch (account.getRiskProfile()) {
            case CONSERVATIVE:
                return holdingCost.compareTo(BigDecimal.valueOf(10_000)) <= 0;
            case MODERATE:
                return holdingCost.compareTo(BigDecimal.valueOf(50_000)) <= 0;
            case AGGRESSIVE:
                return true;  // No limit
            default:
                return false;
        }
    }

    /**
     * Master validation method - validates all business rules
     * Returns ValidationResult containing success status and failure reason (if any)
     * 
     * @param order the order to validate
     * @param account the account placing the order
     * @return ValidationResult with success status and reason for failure
     */
    public ValidationResult validate(TradeOrder order, Account account) {
        // Check quantity
        if (!validateOrderQuantity(order.getQuantity())) {
            String reason = "Invalid quantity: must be greater than 0";
            logger.warn("Order ID: {} failed validation: {}", order.getId(), reason);
            return new ValidationResult(false, reason);
        }
        
        // Check risk profile
        if (!validateRiskProfile(account, order)) {
            String limit = switch (account.getRiskProfile()) {
                case CONSERVATIVE -> "$10,000";
                case MODERATE -> "$50,000";
                case AGGRESSIVE -> "unlimited";
            };
            BigDecimal holdingCost = order.getPrice().multiply(order.getQuantity());
            String reason = String.format("Trade violates %s risk profile limit (max %s per holding, this trade: $%.2f)", 
                account.getRiskProfile(), limit, holdingCost);
            logger.warn("Order ID: {} failed validation: {}", order.getId(), reason);
            return new ValidationResult(false, reason);
        }
        
        // Check side-specific rules
        if (order.getSide() == TradeOrder.OrderSide.BUY) {
            BigDecimal requiredCash = order.getPrice().multiply(order.getQuantity());
            if (!validateAccountHasSufficientCash(account, requiredCash)) {
                String reason = String.format("Insufficient cash available: need $%.2f, have $%.2f", 
                    requiredCash, account.getCashAvailable());
                logger.warn("Order ID: {} failed validation: {}", order.getId(), reason);
                return new ValidationResult(false, reason);
            }
        } 
        // SELL
        else {  
            if (!validateAccountHasInstrument(account, order.getInstrument())) {
                String reason = "Account does not hold " + order.getInstrument().getInstrumentSymbol();
                logger.warn("Order ID: {} failed validation: {}", order.getId(), reason);
                return new ValidationResult(false, reason);
            }
            if (!validateAccountHasSufficientQuantity(account, order.getInstrument(), order.getQuantity())) {
                Holding holding = account.getHoldings().stream()
                    .filter(h -> h.getInstrument().getId().equals(order.getInstrument().getId()))
                    .findFirst()
                    .orElse(null);
                BigDecimal available = holding != null ? holding.getQuantity() : BigDecimal.ZERO;
                String reason = String.format("Insufficient quantity: need %.4f, have %.4f", 
                    order.getQuantity(), available);
                logger.warn("Order ID: {} failed validation: {}", order.getId(), reason);
                return new ValidationResult(false, reason);
            }
        }
        
        // Validation passed
        logger.info("Order ID: {} passed all validation checks", order.getId());
        return new ValidationResult(true, null);
    }

    /**
     * Encapsulates validation result with success status and optional reason
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}