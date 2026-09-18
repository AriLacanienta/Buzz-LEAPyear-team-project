package com.buzzleapyear.trading_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class TestServiceTests {

	private TestService testService = new TestService();

	@Test
	void testPrintReturnsSuccessMessage() {
		String result = testService.testPrint();
		assertEquals("Test successful", result);
	}
}
