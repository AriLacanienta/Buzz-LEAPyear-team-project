package com.buzzleapyear.trading_api.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.buzzleapyear.trading_api.service.QuoteService;
import com.buzzleapyear.trading_api.dto.QuoteResponseDto;
import java.util.List;

@RestController
@RequestMapping("api/v1/quote") 
public class QuoteController {
    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping()
    public ResponseEntity<List<QuoteResponseDto>> getAllLatestQuotes() {
        return ResponseEntity.ok(quoteService.getAllLatestQuotes());
    }
    
    @GetMapping("/{symbol}")
    public ResponseEntity<QuoteResponseDto> getLatestQuoteBySymbol(@PathVariable String symbol) {
        return quoteService.getLatestQuoteBySymbol(symbol)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
