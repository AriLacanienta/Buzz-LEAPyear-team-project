package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByAccountId(Long accountId);
    Optional<Holding> findByAccountIdAndInstrumentId(Long accountId, Long instrumentId);
}
