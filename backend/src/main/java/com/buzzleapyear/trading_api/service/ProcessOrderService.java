package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.infrastructure.OrderQueue;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessOrderService
 * Manages order submission and queuing for async processing
 * 
 * @author Ari Lacanienta
 */
@Service
public class ProcessOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderService.class);
    
    private final OrderQueue orderQueue;
    private final OrderProcessor orderProcessor;
    private final TradeOrderRepository tradeOrderRepository;

    public ProcessOrderService(
            OrderQueue orderQueue,
            OrderProcessor orderProcessor,
            TradeOrderRepository tradeOrderRepository) {
        this.orderQueue = orderQueue;
        this.orderProcessor = orderProcessor;
        this.tradeOrderRepository = tradeOrderRepository;
    }

    /**
     * Submit an order for processing
     * - Enqueue the order (per-account serialization)
     * - Trigger async processing via OrderProcessor
     * 
     * @param order the trade order to submit
     */
    public void submitOrder(TradeOrder order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order must be persisted before submission");
        }
        
        logger.info("Submitting order ID: {} for account: {}", order.getId(), order.getAccount().getId());
        
        // Enqueue the order (adds to per-account queue)
        orderQueue.enqueue(order);
        
        // Trigger async processing
        orderProcessor.processOrderAsync(order);
        
        logger.info("Order ID: {} enqueued and async processing initiated", order.getId());
    }
    
    /**
     * Get the current status of an order
     * Returns the latest TradeOrderStatus record for the order
     * 
     * @param orderId the order ID
     * @return the latest TradeOrderStatus, or null if order not found
     */
    public TradeOrderStatus getOrderStatus(Long orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            logger.warn("Order not found: {}", orderId);
            return null;
        }
        
        // Get the latest status (most recent by timeUpdated)
        return order.getStatuses().stream()
            .max((s1, s2) -> s1.getTimeUpdated().compareTo(s2.getTimeUpdated()))
            .orElse(null);
    }
}
