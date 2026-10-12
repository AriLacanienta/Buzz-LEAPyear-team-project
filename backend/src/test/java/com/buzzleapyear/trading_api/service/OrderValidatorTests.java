package com.buzzleapyear.trading_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;

@ExtendWith(SpringExtension.class)
@ExtendWith (MockitoExtension.class)
public class OrderValidatorTests {

	private OrderValidator orderValidator;

	@BeforeEach
	void setUp() {
		orderValidator = new OrderValidator();
	}

	@Test
	void validateBuyOrderAcceptsWhenCashIsSufficient() {
		Account account = accountWithCash("1000.00", "0.00");
		Instrument instrument = instrument(10L, "AAPL");
		TradeOrder order = order(1L, OrderSide.BUY, "2", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertTrue(result.isValid());
		assertEquals(null, result.getReason());
	}

	@Test
	void validateBuyOrderRejectsWhenCashIsInsufficient() {
		Account account = accountWithCash("100.00", "0.00");
		Instrument instrument = instrument(10L, "AAPL");
		TradeOrder order = order(2L, OrderSide.BUY, "2", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertFalse(result.isValid());
		assertTrue(result.getReason().contains("Insufficient cash available"));
	}

	@Test
	void validateSellOrderRejectsWhenHoldingIsMissing() {
		Account account = accountWithCash("500.00", "0.00");
		account.setHoldings(List.of());

		Instrument instrument = instrument(10L, "AAPL");
		TradeOrder order = order(3L, OrderSide.SELL, "1", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertFalse(result.isValid());
		assertTrue(result.getReason().contains("does not hold"));
	}

	@Test
	void validateSellOrderRejectsWhenQuantityIsInsufficient() {
		Account account = accountWithCash("500.00", "0.00");
		Instrument instrument = instrument(10L, "AAPL");
		account.setHoldings(List.of(holding(20L, instrument, "3")));

		TradeOrder order = order(4L, OrderSide.SELL, "5", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertFalse(result.isValid());
		assertTrue(result.getReason().contains("Insufficient quantity"));
	}

	@Test
	void validateSellOrderAcceptsWhenQuantityIsSufficient() {
		Account account = accountWithCash("500.00", "0.00");
		Instrument instrument = instrument(10L, "AAPL");
		account.setHoldings(List.of(holding(21L, instrument, "5")));

		TradeOrder order = order(5L, OrderSide.SELL, "5", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertTrue(result.isValid());
		assertEquals(null, result.getReason());
	}

	@Test
	void validateRejectsNonPositiveQuantity() {
		Account account = accountWithCash("1000.00", "0.00");
		Instrument instrument = instrument(10L, "AAPL");
		TradeOrder order = order(6L, OrderSide.BUY, "0", "100", instrument);

		OrderValidator.ValidationResult result = orderValidator.validate(order, account);

		assertFalse(result.isValid());
		assertEquals("Invalid quantity: must be greater than 0", result.getReason());
	}

	private Account accountWithCash(String available, String reserved) {
		Account account = new Account();
		account.setId(1L);
		account.setCashAvailable(new BigDecimal(available));
		account.setCashReserved(new BigDecimal(reserved));
		return account;
	}

	private TradeOrder order(Long id, OrderSide side, String quantity, String price, Instrument instrument) {
		TradeOrder order = new TradeOrder();
		order.setId(id);
		order.setSide(side);
		order.setQuantity(new BigDecimal(quantity));
		order.setPrice(new BigDecimal(price));
		order.setInstrument(instrument);
		return order;
	}

	private Instrument instrument(Long id, String symbol) {
		Instrument instrument = new Instrument();
		instrument.setId(id);
		instrument.setInstrumentSymbol(symbol);
		return instrument;
	}

	private Holding holding(Long id, Instrument instrument, String quantity) {
		Holding holding = new Holding();
		holding.setId(id);
		holding.setInstrument(instrument);
		holding.setQuantity(new BigDecimal(quantity));
		holding.setTotalCost(BigDecimal.ZERO);
		return holding;
	}
}
