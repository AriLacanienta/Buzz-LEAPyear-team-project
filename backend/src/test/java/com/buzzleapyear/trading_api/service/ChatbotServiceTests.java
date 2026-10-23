package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTests {
    @Mock private AccountService accountService;
    @Mock private QuoteService quoteService;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private OrderProcessor orderProcessor;
    @InjectMocks private ChatbotService chatbotService;

    @ParameterizedTest
    @ValueSource(strings = {"raise a dispute", "ADD A DISPUTE", "dispute order 7",
        "dispute my buy 5 shares of AAPL", "dispute my portfolio quote"})
    void disputeRequestsOnlyOpenAFormBeforeOtherIntentParsing(String message) {
        var response = chatbotService.respond(message, "owner");
        assertEquals("DISPUTE_FORM", response.intent());
        assertEquals("CREATE_DISPUTE", response.action());
        assertNull(response.data());
        verifyNoInteractions(accountService, quoteService, instrumentRepository, orderProcessor);
    }

    @Test
    void helpAdvertisesDisputesWithoutOpeningAForm() {
        var response = chatbotService.respond("help", "owner");
        assertEquals("HELP", response.intent());
        assertTrue(response.message().contains("raise a dispute"));
    }

    @Test
    void existingQuoteRoutingIsPreserved() {
        when(quoteService.getLatestQuoteBySymbol("AAPL")).thenReturn(Optional.empty());
        assertEquals("QUOTE", chatbotService.respond("quote AAPL", "owner").intent());
        verify(quoteService).getLatestQuoteBySymbol("AAPL");
    }

    @Test
    void similarWordsDoNotOpenTheDisputeForm() {
        assertEquals("UNKNOWN", chatbotService.respond("undisputed", "owner").intent());
    }
}