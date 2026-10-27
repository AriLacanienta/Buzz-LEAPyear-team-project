package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.RecentOrderResponseDTO;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.service.AccountService;
import com.buzzleapyear.trading_api.service.OrderProcessor;
import com.buzzleapyear.trading_api.service.ProcessOrderService;
import com.buzzleapyear.trading_api.service.TradeOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerTests {

    @Mock
    private ProcessOrderService processOrderService;

    @Mock
    private TradeOrderRepository tradeOrderRepository;

    @Mock
    private TradeOrderStatusRepository tradeOrderStatusRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private TradeOrderService tradeOrderService;

    @Mock
    private AccountService accountService;

    @Mock
    private OrderProcessor orderProcessor;

    @InjectMocks
    private TradeOrderController tradeOrderController;

    private final Authentication authentication =
        new UsernamePasswordAuthenticationToken("C002", null, List.of());

    @Test
    void getRecentOrdersReturnsNotFoundForAccountOwnedByAnotherUser() {
        when(accountService.findAccountByIdForUser(1L, "C002")).thenReturn(Optional.empty());

        ResponseEntity<List<RecentOrderResponseDTO>> response =
            tradeOrderController.getRecentOrders(1L, 20, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(tradeOrderService, never()).getRecentOrdersByAccountId(anyLong(), anyInt());
    }

    @Test
    void getRecentOrdersReturnsOrdersForOwnAccount() {
        when(accountService.findAccountByIdForUser(2L, "C002")).thenReturn(Optional.of(new Account()));
        when(tradeOrderService.getRecentOrdersByAccountId(2L, 20)).thenReturn(List.of());

        ResponseEntity<List<RecentOrderResponseDTO>> response =
            tradeOrderController.getRecentOrders(2L, 20, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
    }
}
