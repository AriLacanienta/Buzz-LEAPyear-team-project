package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.HoldingResponseDto;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HoldingService {
    private final HoldingRepository holdingRepository;
    private final QuoteService quoteService;

    public HoldingService(HoldingRepository holdingRepository, QuoteService quoteService) {
        this.holdingRepository = holdingRepository;
        this.quoteService = quoteService;
    }

    public List<HoldingResponseDto> getHoldingsByAccountId(Long accountId) {
        List<Holding> holdings = holdingRepository.findByAccountId(accountId);
        
        return holdings.stream().map(this::convertToDto).filter(dto -> dto != null).collect(Collectors.toList());
    }

    private HoldingResponseDto convertToDto(Holding holding) {
        String symbol = holding.getInstrument().getInstrumentSymbol();
        
        var quoteDto = quoteService.getLatestQuoteBySymbol(symbol);

        BigDecimal price = BigDecimal.ZERO;
        BigDecimal change = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;
        
        if (quoteDto.isPresent()) {
            var quote = quoteDto.get();
            price = quote.price();
            change = quote.change();
            changePercent = quote.changePercent();
        }
        
        BigDecimal totalValue = holding.getQuantity().multiply(price);
        
        return new HoldingResponseDto(
            holding.getInstrument().getInstrumentName(),
            symbol,
            holding.getQuantity(),
            price,
            totalValue,
            change,
            changePercent,
            holding.getTotalCost()
        );
    }

    public List<HoldingResponseDto> getAllHoldings() {
        List<Holding> holdings = holdingRepository.findAll();
        
        return holdings.stream().map(this::convertToDto).collect(Collectors.toList());
    }
}