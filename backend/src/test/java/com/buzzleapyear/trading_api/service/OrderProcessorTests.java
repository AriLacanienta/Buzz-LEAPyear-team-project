package com.buzzleapyear.trading_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.HoldingRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;

@ExtendWith(MockitoExtension.class)
public class OrderProcessorTests {

    @Mock
    private TradeOrderRepository tradeOrderRepository;

    @Mock
    private TradeOrderStatusRepository tradeOrderStatusRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private OrderExecutor orderExecutor;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() throws Exception {
        orderProcessor = new OrderProcessor(
            new OrderValidator(),
            orderExecutor,
            tradeOrderRepository,
            tradeOrderStatusRepository,
            holdingRepository,
            accountRepository,
            quoteService);

        Field delayField = OrderProcessor.class.getDeclaredField("orderExecutionDelayMs");
        delayField.setAccessible(true);
        delayField.set(orderProcessor, 0L);
    }

    @Test
    void processBuyOrderLogsAcceptedAndFilledAndReservesCash() {
        Account account = account(1L, "1000.00", "0.00");
        Instrument instrument = instrument(1L, "AAPL");
        TradeOrder order = order(100L, account, instrument, OrderSide.BUY, "2", "100.00");

        when(tradeOrderRepository.findByIdForProcessing(100L)).thenReturn(Optional.of(order));
        when(quoteService.getLatestQuoteSnapshotBySymbol("AAPL"))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("100.00"), LocalDateTime.now())))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("100.00"), LocalDateTime.now())));

        orderProcessor.processOrder(100L);

        assertEquals(new BigDecimal("800.00"), account.getCashAvailable());
        assertEquals(new BigDecimal("200.00"), account.getCashReserved());

        ArgumentCaptor<TradeOrderStatus> statusCaptor = ArgumentCaptor.forClass(TradeOrderStatus.class);
        verify(tradeOrderStatusRepository, atLeast(2)).save(statusCaptor.capture());

        List<OrderStatus> statuses = statusCaptor.getAllValues().stream()
            .map(TradeOrderStatus::getStatus)
            .toList();
        assertTrue(statuses.contains(OrderStatus.ACCEPTED));
        assertTrue(statuses.contains(OrderStatus.FILLED));

        verify(orderExecutor).executeBuyOrder(account, instrument, order, new BigDecimal("100.00"));
    }

    @Test
    void processOrderLogsRejectedWhenValidationFails() {
        Account account = account(2L, "10.00", "0.00");
        Instrument instrument = instrument(2L, "AAPL");
        TradeOrder order = order(101L, account, instrument, OrderSide.BUY, "2", "100.00");

        when(tradeOrderRepository.findByIdForProcessing(101L)).thenReturn(Optional.of(order));
        when(quoteService.getLatestQuoteSnapshotBySymbol("AAPL"))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("100.00"), LocalDateTime.now())));

        orderProcessor.processOrder(101L);

        ArgumentCaptor<TradeOrderStatus> statusCaptor = ArgumentCaptor.forClass(TradeOrderStatus.class);
        verify(tradeOrderStatusRepository).save(statusCaptor.capture());

        assertEquals(OrderStatus.REJECTED, statusCaptor.getValue().getStatus());
        assertTrue(statusCaptor.getValue().getReasonText().contains("Insufficient cash available"));
        verify(orderExecutor, never()).executeBuyOrder(any(), any(), any(), any());
    }

    @Test
    void processOrderLogsRejectedAndRestoresReservedCashWhenExecutionFailsAfterAcceptance() {
        Account account = account(3L, "500.00", "0.00");
        Instrument instrument = instrument(3L, "AAPL");
        TradeOrder order = order(102L, account, instrument, OrderSide.BUY, "2", "100.00");

        when(tradeOrderRepository.findByIdForProcessing(102L)).thenReturn(Optional.of(order));
        when(quoteService.getLatestQuoteSnapshotBySymbol("AAPL"))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("100.00"), LocalDateTime.now())))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("100.00"), LocalDateTime.now())));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));
        doThrow(new RuntimeException("db write failed"))
            .when(orderExecutor).executeBuyOrder(account, instrument, order, new BigDecimal("100.00"));

        orderProcessor.processOrder(102L);

        ArgumentCaptor<TradeOrderStatus> statusCaptor = ArgumentCaptor.forClass(TradeOrderStatus.class);
        verify(tradeOrderStatusRepository, atLeast(2)).save(statusCaptor.capture());

        List<TradeOrderStatus> statuses = statusCaptor.getAllValues();
        OrderStatus lastStatus = statuses.get(statuses.size() - 1).getStatus();
        assertEquals(OrderStatus.REJECTED, lastStatus);

        assertEquals(new BigDecimal("500.00"), account.getCashAvailable());
        assertEquals(new BigDecimal("0.00"), account.getCashReserved());
    }

    @Test
    void processSellOrderLogsAcceptedAndFilled() {
        Account account = account(4L, "200.00", "0.00");
        Instrument instrument = instrument(4L, "MSFT");
        Holding holding = new Holding();
        holding.setId(1L);
        holding.setInstrument(instrument);
        holding.setQuantity(new BigDecimal("10"));
        holding.setTotalCost(new BigDecimal("800"));
        account.setHoldings(new ArrayList<>(List.of(holding)));

        TradeOrder order = order(103L, account, instrument, OrderSide.SELL, "3", "120.00");

        when(tradeOrderRepository.findByIdForProcessing(103L)).thenReturn(Optional.of(order));
        when(quoteService.getLatestQuoteSnapshotBySymbol("MSFT"))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("120.00"), LocalDateTime.now())))
            .thenReturn(Optional.of(new QuoteService.QuoteSnapshot(new BigDecimal("120.00"), LocalDateTime.now())));

        orderProcessor.processOrder(103L);

        ArgumentCaptor<TradeOrderStatus> statusCaptor = ArgumentCaptor.forClass(TradeOrderStatus.class);
        verify(tradeOrderStatusRepository, atLeast(2)).save(statusCaptor.capture());

        List<OrderStatus> statuses = statusCaptor.getAllValues().stream()
            .map(TradeOrderStatus::getStatus)
            .toList();
        assertTrue(statuses.contains(OrderStatus.ACCEPTED));
        assertTrue(statuses.contains(OrderStatus.FILLED));

        verify(orderExecutor).executeSellOrder(account, instrument, order);
    }

    private Account account(Long id, String available, String reserved) {
        Account account = new Account();
        account.setId(id);
        account.setCashAvailable(new BigDecimal(available));
        account.setCashReserved(new BigDecimal(reserved));
        account.setHoldings(new ArrayList<>());
        return account;
    }

    private Instrument instrument(Long id, String symbol) {
        Instrument instrument = new Instrument();
        instrument.setId(id);
        instrument.setInstrumentSymbol(symbol);
        return instrument;
    }

    private TradeOrder order(Long id, Account account, Instrument instrument, OrderSide side, String quantity, String price) {
        TradeOrder order = new TradeOrder();
        order.setId(id);
        order.setAccount(account);
        order.setInstrument(instrument);
        order.setSide(side);
        order.setQuantity(new BigDecimal(quantity));
        order.setPrice(new BigDecimal(price));
        order.setValue(new BigDecimal(quantity).multiply(new BigDecimal(price)));
        order.setOrderDate(LocalDateTime.now());
        return order;
    }
}
