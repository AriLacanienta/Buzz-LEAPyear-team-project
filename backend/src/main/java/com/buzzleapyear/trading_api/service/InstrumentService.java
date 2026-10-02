package com.buzzleapyear.trading_api.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Arrays;
import java.math.BigDecimal;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;

@Service
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findAll();
    }

    public List<Instrument> getAllInstrumentsByAssetTypeAsc(AssetType assetType) {
        return instrumentRepository.findByAssetTypeOrderByInstrumentSymbolAsc(assetType);
    }

    public Optional<Instrument> getInstrumentBySymbol(String symbol) {
        return instrumentRepository.findByInstrumentSymbol(symbol);
    }
}
