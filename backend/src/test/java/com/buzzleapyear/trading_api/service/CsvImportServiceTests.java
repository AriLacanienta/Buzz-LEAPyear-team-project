package com.buzzleapyear.trading_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.buzzleapyear.trading_api.service.ImportCSVService.LineValues;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

@ExtendWith(SpringExtension.class)
class CsvImportServiceTests {

	private ImportCSVService testService;

    @BeforeEach
    void setUp(){
        testService = new ImportCSVService();
    }

    @Nested
    class ParsingTests {

        @Test
        void testHappyPath() {
            final String TEST_LINE = "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40\r\n";
            LineValues result = testService.parseLine(TEST_LINE);

            Assertions.assertAll("parsed values",
                () -> Assertions.assertEquals("Alice Chen", result.client_name)
            );
        }
        
    }
}
