package com.buzzleapyear.trading_api.infrastructure;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * OrderQueue maintains a per-account queue of trade orders
 * ensuring sequential processing for each account
 * 
 * @author Ari Lacanienta
 */
@Component
public class OrderQueue {
    
    // Map of accountId -> BlockingQueue of TradeOrder
    private final ConcurrentHashMap<Long, BlockingQueue<TradeOrder>> accountQueues = new ConcurrentHashMap<>();

    /**
     * Enqueue a trade order for the given account
     * @param order the TradeOrder to enqueue
     */
    public void enqueue(TradeOrder order) {
        if (order == null || order.getAccount() == null || order.getAccount().getId() == null) {
            throw new IllegalArgumentException("Order and account ID must not be null");
        }
        
        Long accountId = order.getAccount().getId();
        BlockingQueue<TradeOrder> queue = accountQueues.computeIfAbsent(
            accountId,
            k -> new LinkedBlockingQueue<>()
        );
        
        try {
            queue.put(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while enqueuing order", e);
        }
    }

    /**
     * Dequeue the next order for a given account (blocking)
     * @param accountId the account ID
     * @return the next TradeOrder, or null if no queue exists for the account
     */
    public TradeOrder dequeue(Long accountId) throws InterruptedException {
        BlockingQueue<TradeOrder> queue = accountQueues.get(accountId);
        if (queue == null) {
            return null;
        }
        return queue.take();
    }

    /**
     * Peek at the next order for a given account without removing it
     * @param accountId the account ID
     * @return the next TradeOrder, or null if queue is empty or doesn't exist
     */
    public TradeOrder peek(Long accountId) {
        BlockingQueue<TradeOrder> queue = accountQueues.get(accountId);
        if (queue == null) {
            return null;
        }
        return queue.peek();
    }

    /**
     * Get the size of the queue for a given account
     * @param accountId the account ID
     * @return the number of orders queued for the account
     */
    public int size(Long accountId) {
        BlockingQueue<TradeOrder> queue = accountQueues.get(accountId);
        if (queue == null) {
            return 0;
        }
        return queue.size();
    }
}
