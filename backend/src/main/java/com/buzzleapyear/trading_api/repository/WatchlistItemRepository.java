package com.buzzleapyear.trading_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.WatchlistItem;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUserOrderByCreatedAtAsc(User user);
    Optional<WatchlistItem> findByUserAndInstrument(User user, Instrument instrument);
}
