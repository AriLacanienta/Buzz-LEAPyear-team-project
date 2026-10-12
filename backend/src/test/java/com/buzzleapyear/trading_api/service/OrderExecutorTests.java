package com.buzzleapyear.trading_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.HoldingRepository;

@ExtendWith(MockitoExtension.class)
public class OrderExecutorTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    private OrderExecutor orderExecutor;

    @BeforeEach
    void setUp() {
        orderExecutor = new OrderExecutor(accountRepository, holdingRepository);
    }

    @Test
    void executeBuyOrderUpdatesCashAndHolding() {
        Account account = account(1L, "1000.00", "200.00");
        Instrument instrument = instrument(1L, "AAPL");
        TradeOrder order = order(10L, OrderSide.BUY, instrument, "2", "90.00");

        Holding existingHolding = holding(20L, account, instrument, "5", "500.00");
        when(holdingRepository.findByAccountIdAndInstrumentId(1L, 1L)).thenReturn(Optional.of(existingHolding));

        orderExecutor.executeBuyOrder(account, instrument, order, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("1020.00"), account.getCashAvailable());
        assertEquals(new BigDecimal("0.00"), account.getCashReserved());
        assertEquals(new BigDecimal("7"), existingHolding.getQuantity());
        assertEquals(new BigDecimal("680.00"), existingHolding.getTotalCost());
        verify(accountRepository).save(account);
        verify(holdingRepository).save(existingHolding);
    }

    @Test
    void executeBuyOrderThrowsWhenExecutionPriceNeedsMoreCashThanAvailable() {
        Account account = account(1L, "5.00", "200.00");
        Instrument instrument = instrument(1L, "AAPL");
        TradeOrder order = order(11L, OrderSide.BUY, instrument, "2", "110.00");

        when(holdingRepository.findByAccountIdAndInstrumentId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> orderExecutor.executeBuyOrder(account, instrument, order, new BigDecimal("100.00")));

        verify(accountRepository, never()).save(account);
    }

    @Test
    void executeSellOrderUpdatesHoldingAndCashAvailable() {
        Account account = account(2L, "300.00", "0.00");
        account.setHoldings(new ArrayList<>());

        Instrument instrument = instrument(2L, "MSFT");
        Holding existingHolding = holding(30L, account, instrument, "10", "1000.00");
        account.getHoldings().add(existingHolding);

        TradeOrder order = order(12L, OrderSide.SELL, instrument, "4", "150.00");
        when(holdingRepository.findByAccountIdAndInstrumentId(2L, 2L)).thenReturn(Optional.of(existingHolding));

        orderExecutor.executeSellOrder(account, instrument, order);

        assertEquals(new BigDecimal("6"), existingHolding.getQuantity());
        assertEquals(new BigDecimal("600.0000000000"), existingHolding.getTotalCost());
        assertEquals(new BigDecimal("900.00"), account.getCashAvailable());
        verify(holdingRepository).save(existingHolding);
        verify(accountRepository).save(account);
    }

    @Test
    void executeSellOrderDeletesHoldingWhenFullySold() {
        Account account = account(3L, "300.00", "0.00");
        account.setHoldings(new ArrayList<>());

        Instrument instrument = instrument(3L, "TSLA");
        Holding existingHolding = holding(31L, account, instrument, "5", "500.00");
        account.getHoldings().add(existingHolding);

        TradeOrder order = order(13L, OrderSide.SELL, instrument, "5", "120.00");
        when(holdingRepository.findByAccountIdAndInstrumentId(3L, 3L)).thenReturn(Optional.of(existingHolding));

        orderExecutor.executeSellOrder(account, instrument, order);

        assertEquals(0, account.getHoldings().size());
        assertEquals(new BigDecimal("900.00"), account.getCashAvailable());
        verify(holdingRepository).delete(existingHolding);
        verify(accountRepository).save(account);
    }

    private Account account(Long id, String available, String reserved) {
        Account account = new Account();
        account.setId(id);
        account.setCashAvailable(new BigDecimal(available));
        account.setCashReserved(new BigDecimal(reserved));
        return account;
    }

    private Instrument instrument(Long id, String symbol) {
        Instrument instrument = new Instrument();
        instrument.setId(id);
        instrument.setInstrumentSymbol(symbol);
        return instrument;
    }

    private TradeOrder order(Long id, OrderSide side, Instrument instrument, String qty, String price) {
        TradeOrder order = new TradeOrder();
        order.setId(id);
        order.setSide(side);
        order.setInstrument(instrument);
        order.setQuantity(new BigDecimal(qty));
        order.setPrice(new BigDecimal(price));
        return order;
    }

    private Holding holding(Long id, Account account, Instrument instrument, String qty, String totalCost) {
        Holding holding = new Holding();
        holding.setId(id);
        holding.setAccount(account);
        holding.setInstrument(instrument);
        holding.setQuantity(new BigDecimal(qty));
        holding.setTotalCost(new BigDecimal(totalCost));
        return holding;
    }
}
