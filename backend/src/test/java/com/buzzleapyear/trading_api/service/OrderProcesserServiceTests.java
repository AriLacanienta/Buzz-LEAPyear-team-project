package com.buzzleapyear.trading_api.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderProcesserServiceTests {

    @Mock
    private OrderProcessor orderProcessor;

    private ProcessOrderService processOrderService;

    @BeforeEach
    void setUp() {
        processOrderService = new ProcessOrderService(orderProcessor);
    }

    @Test
    void submitOrderDelegatesToOrderProcessor() {
        processOrderService.submitOrder(99L);

        verify(orderProcessor).processOrder(99L);
    }

    @Test
    void submitOrderThrowsWhenOrderIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> processOrderService.submitOrder(null));
    }
}
