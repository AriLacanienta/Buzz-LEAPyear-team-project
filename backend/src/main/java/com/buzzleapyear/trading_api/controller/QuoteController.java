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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/quote") 
public class QuoteController {
    private final QuoteService quoteService;
    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

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

    // GET /api/v1/quote/market?assetType={assetType}&market={market}&page={page}&size={size}
    @GetMapping("/market")
    public ResponseEntity<Page<ListMarketEquitySummaryResponseDto>> getLatestMarketEquityQuotes(
        @RequestParam AssetType assetType,
        @RequestParam(required = false) String market,
        Pageable pageable
    ) {
        logger.info("Received request - assetType: {}, market: {}, page: {}, size: {}", assetType, market, pageable.getPageNumber(), pageable.getPageSize());
        
        String currencyCode = null;
        if (market != null && !market.isEmpty()) {
            currencyCode = switch (market.toUpperCase()) {
                case "US" -> "USD";
                case "UK" -> "GBP";
                case "INDIA", "INDIAN" -> "INR";
                default -> null;
            };
        }
        
        logger.info("Converted market: {} to currencyCode: {}", market, currencyCode);
        
        return ResponseEntity.ok(quoteService.getLatestMarketEquityQuotes(assetType, currencyCode, pageable));
    }
}
