package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    Optional<Instrument> findByInstrumentSymbol(String symbol);

    List<Instrument> findByAssetTypeOrderByInstrumentSymbolAsc(AssetType assetType);
}
