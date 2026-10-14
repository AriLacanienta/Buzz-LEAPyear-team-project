package com.buzzleapyear.trading_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

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
    public ProcessOrderService(OrderProcessor orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    /**
     * Submit an order for processing
     * 
     * @param orderId the trade order to submit
     */
    @Async
    public void submitOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        logger.info("Scheduling order ID: {} for processing", orderId);
        orderProcessor.processOrder(orderId);
    }
}
