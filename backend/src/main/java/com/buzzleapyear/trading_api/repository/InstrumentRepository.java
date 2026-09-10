package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    Optional<Instrument> findByInstrumentSymbol(String symbol);
}
