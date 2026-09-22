package com.buzzleapyear.trading_api.service;

import org.springframework.stereotype.Service;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;

/**
 * 
 * ProcessOrderService
 * @author Ari Lacanienta
 */
@Service 
public class ProcessOrderService {
    
    

    public ProcessOrderService(){
    }

    public void submitOrder(TradeOrder order) {
        TradeOrderStatus status = new TradeOrderStatus();
        status.setTradeOrder(order);
        status.setStatus(OrderStatus.SUBMITTED);
        // Validate Order
        

        // Execute Order

        // Update holdings etc.
    }


}
