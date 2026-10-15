package com.buzzleapyear.trading_api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
// for manual testing:
//curl --json '{"side":"BUY","price":"9999.99","accountId":"1","instrumentId":"1","quantity":"999"}' http://localhost:6767/api/v1/tradeorders

public class OrderProcesserServiceTests {
    
    @Test
    void sampleTest() {
        // verify(mockEntityManager, times(2)).persist(any(TradeOrderStatus.class));
        assertTrue(true);
    }
}
