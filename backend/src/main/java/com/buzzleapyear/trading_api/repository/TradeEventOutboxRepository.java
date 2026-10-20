package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.TradeEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TradeEventOutboxRepository
    extends JpaRepository<TradeEventOutbox, UUID> {
        List<TradeEventOutbox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}