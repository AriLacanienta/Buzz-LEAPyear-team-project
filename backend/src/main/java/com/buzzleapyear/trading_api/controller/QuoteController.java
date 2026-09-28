package com.buzzleapyear.trading_api.controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.buzzleapyear.trading_api.service.QuoteService;
import com.buzzleapyear.trading_api.dto.QuoteResponseDto;
import com.buzzleapyear.trading_api.dto.ListMarketEquitySummaryResponseDto;
import java.util.List;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;

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

    // GET /api/v1/quote/market?assetType={assetType}&page={page}&size={size}
    @GetMapping("/market")
    public ResponseEntity<Page<ListMarketEquitySummaryResponseDto>> getLatestMarketEquityQuotes(
        @RequestParam AssetType assetType,
        Pageable pageable
    ) {
        return ResponseEntity.ok(quoteService.getLatestMarketEquityQuotes(assetType, pageable));
    }
}
