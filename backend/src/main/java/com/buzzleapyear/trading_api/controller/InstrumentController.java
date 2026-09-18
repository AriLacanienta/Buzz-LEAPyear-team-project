package com.buzzleapyear.trading_api.controller;
import com.buzzleapyear.trading_api.util.InstrumentUtil;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.buzzleapyear.trading_api.service.InstrumentService;
import com.buzzleapyear.trading_api.dto.InstrumentDetailsResponseDto;
import com.buzzleapyear.trading_api.dto.ListInstrumentsResponseDto;
import com.buzzleapyear.trading_api.entity.Instrument;
import org.springframework.http.ResponseEntity;
import com.buzzleapyear.trading_api.mapper.InstrumentMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;



@RestController
@RequestMapping("api/v1/instruments") 
public class InstrumentController {
    private final InstrumentService instrumentService;
    private final InstrumentMapper instrumentMapper;

    public InstrumentController(InstrumentService instrumentService, InstrumentMapper instrumentMapper) {
        this.instrumentService = instrumentService;
        this.instrumentMapper = instrumentMapper;
    }

    @GetMapping()
    public ResponseEntity<Page<ListInstrumentsResponseDto>> getAllInstruments(Pageable pageable) {
        List<ListInstrumentsResponseDto> mockData = InstrumentUtil.getMockInstruments();

        // Create a Page using the mock data
        Page<ListInstrumentsResponseDto> page = new PageImpl<>(
            mockData,
            pageable,
            mockData.size()
        );

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{symbol}/details")
    public ResponseEntity<InstrumentDetailsResponseDto> getInstrumentBySymbol(@PathVariable String symbol) {
        Optional<Instrument> instrument = instrumentService.getInstrumentBySymbol(symbol);

        return instrument
        .map(instrumentMapper::toInstrumentDetailsResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
    }
}
