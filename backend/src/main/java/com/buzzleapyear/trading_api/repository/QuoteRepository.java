package com.buzzleapyear.trading_api.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.buzzleapyear.trading_api.entity.Quote;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    Optional<Quote> findTopByInstrumentIdOrderByTimestampDesc(Long instrumentId);
}
