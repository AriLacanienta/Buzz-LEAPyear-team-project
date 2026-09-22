package com.buzzleapyear.trading_api.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import jakarta.persistence.EntityManager;

@ExtendWith(SpringExtension.class)
public class OrderValidatorTests {
    

    private OrderValidator validator;

    @Mock 
    private EntityManager mockEntityManager;

    private TradeOrder testOrder;

    @BeforeEach
    void setUp() {
        this.validator = new OrderValidator();
    } 

    @Test 
        void validatesGoodTradeOrder(){
    }




    
}
