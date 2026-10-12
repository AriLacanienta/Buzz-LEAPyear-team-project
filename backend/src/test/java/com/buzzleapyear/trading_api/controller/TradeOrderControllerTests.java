package com.buzzleapyear.trading_api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.buzzleapyear.trading_api.dto.OrderStatusDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderResponseDTO;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.service.AccountService;
import com.buzzleapyear.trading_api.service.OrderProcessor;
import com.buzzleapyear.trading_api.service.ProcessOrderService;
import com.buzzleapyear.trading_api.service.TradeOrderService;

@ExtendWith(MockitoExtension.class)
public class TradeOrderControllerTests {

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

    @Mock
    private Authentication authentication;

    private TradeOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new TradeOrderController(
            processOrderService,
            tradeOrderRepository,
            tradeOrderStatusRepository,
            accountRepository,
            instrumentRepository,
            tradeOrderService,
            accountService,
            orderProcessor);
    }

    @Test
    void submitOrderReturnsAcceptedAndLogsSubmittedStatus() {
        Account account = new Account();
        account.setId(1L);
        account.setCashAvailable(new BigDecimal("1000.00"));
        account.setCashReserved(BigDecimal.ZERO);

        Instrument instrument = new Instrument();
        instrument.setId(10L);
        instrument.setInstrumentSymbol("AAPL");

        TradeOrder savedOrder = new TradeOrder();
        savedOrder.setId(100L);
        savedOrder.setAccount(account);
        savedOrder.setInstrument(instrument);
        savedOrder.setSide(OrderSide.BUY);
        savedOrder.setQuantity(new BigDecimal("2"));
        savedOrder.setPrice(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn("demo-user");
        when(accountService.findAccountByIdForUser(1L, "demo-user")).thenReturn(Optional.of(account));
        when(instrumentRepository.findByInstrumentSymbol("AAPL")).thenReturn(Optional.of(instrument));
        when(tradeOrderRepository.save(org.mockito.ArgumentMatchers.any(TradeOrder.class))).thenReturn(savedOrder);

        TradeOrderDTO dto = new TradeOrderDTO(
            OrderSide.BUY,
            "AAPL",
            new BigDecimal("2"),
            new BigDecimal("100.00"),
            1L);

        ResponseEntity<TradeOrderResponseDTO> response = controller.submitOrder(dto, authentication);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.SUBMITTED, response.getBody().getStatus());
        assertEquals(100L, response.getBody().getOrderId());

        ArgumentCaptor<TradeOrderStatus> statusCaptor = ArgumentCaptor.forClass(TradeOrderStatus.class);
        verify(tradeOrderStatusRepository).save(statusCaptor.capture());
        assertEquals(OrderStatus.SUBMITTED, statusCaptor.getValue().getStatus());

        verify(processOrderService).submitOrder(100L);
    }

    @Test
    void getOrderStatusReturnsLatestStatusForOwner() {
        Account account = new Account();
        account.setId(4L);

        TradeOrder order = new TradeOrder();
        order.setId(200L);
        order.setAccount(account);

        TradeOrderStatus latest = new TradeOrderStatus();
        latest.setStatus(OrderStatus.FILLED);
        latest.setReasonText(null);
        latest.setTimeUpdated(LocalDateTime.now());

        when(authentication.getName()).thenReturn("demo-user");
        when(tradeOrderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(accountService.findAccountByIdForUser(4L, "demo-user")).thenReturn(Optional.of(account));
        when(tradeOrderStatusRepository.getLatestStatusById(200L)).thenReturn(latest);

        ResponseEntity<OrderStatusDTO> response = controller.getOrderStatus(200L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.FILLED, response.getBody().getStatus());
        assertEquals(200L, response.getBody().getOrderId());
        assertEquals(4L, response.getBody().getAccountId());
    }

    @Test
    void getOrderStatusReturnsNotFoundWhenOrderIsNotOwnedByRequester() {
        Account account = new Account();
        account.setId(4L);

        TradeOrder order = new TradeOrder();
        order.setId(201L);
        order.setAccount(account);

        when(authentication.getName()).thenReturn("demo-user");
        when(tradeOrderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(accountService.findAccountByIdForUser(4L, "demo-user")).thenReturn(Optional.empty());

        ResponseEntity<OrderStatusDTO> response = controller.getOrderStatus(201L, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
