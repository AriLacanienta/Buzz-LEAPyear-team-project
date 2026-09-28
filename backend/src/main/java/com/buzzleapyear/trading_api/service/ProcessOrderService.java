package com.buzzleapyear.trading_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.buzzleapyear.trading_api.entity.TradeOrder;

/**
 * ProcessOrderService
 * Manages order submission and processing
 * 
 * @author Ari Lacanienta
 */
@Service
public class ProcessOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderService.class);
    
    private final OrderProcessor orderProcessor;
    public ProcessOrderService(
            OrderProcessor orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    /**
     * Submit an order for processing
     * 
     * @param order the trade order to submit
     */
    public void submitOrder(TradeOrder order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order must be persisted before submission");
        }
        
        logger.info("Submitting order ID: {} for account: {}", order.getId(), order.getAccount().getId());
        
        orderProcessor.processOrder(order);
        
    }
}
