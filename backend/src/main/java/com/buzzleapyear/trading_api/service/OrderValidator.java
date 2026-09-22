package com.buzzleapyear.trading_api.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.buzzleapyear.trading_api.entity.TradeOrder;

/**
 * OrderValidator
 * @author Ari Lacanienta
 */
@Component 
public class OrderValidator {


    public OrderValidator(){
    }

    public boolean validate(TradeOrder order) {
        return (order.getQuantity().compareTo(BigDecimal.ZERO) <= 0);
    }
    
}