package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Pricing;
import com.buzzleapyear.trading_api.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingRepository extends JpaRepository<Pricing, Long> {
    
    /**
     * Find the latest pricing record for a given instrument
     * @param instrument the instrument to find pricing for
     * @return Optional containing the latest pricing record
     */
    @Query(value = "SELECT * FROM pricing WHERE instrument_id = :instrumentId ORDER BY price_date DESC LIMIT 1", nativeQuery = true)
    Optional<Pricing> findLatestByInstrumentId(@Param("instrumentId") Long instrumentId);
    
    /**
     * Find the latest pricing record for a given instrument entity
     * @param instrument the instrument to find pricing for
     * @return Optional containing the latest pricing record
     */
    @Query("SELECT p FROM Pricing p WHERE p.instrument = :instrument ORDER BY p.priceDate DESC LIMIT 1")
    Optional<Pricing> findLatestByInstrument(@Param("instrument") Instrument instrument);
}
