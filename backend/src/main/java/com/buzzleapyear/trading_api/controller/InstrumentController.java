package com.buzzleapyear.trading_api.controller;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.dto.InstrumentDetailsResponseDto;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.mapper.InstrumentMapper;
import com.buzzleapyear.trading_api.service.InstrumentService;



@RestController
@RequestMapping("/api/v1/instruments") 
public class InstrumentController {
    private final InstrumentService instrumentService;
    private final InstrumentMapper instrumentMapper;

    public InstrumentController(InstrumentService instrumentService, InstrumentMapper instrumentMapper) {
        this.instrumentService = instrumentService;
        this.instrumentMapper = instrumentMapper;
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
