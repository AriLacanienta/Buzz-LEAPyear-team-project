package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeOrderStatusRepository extends JpaRepository<TradeOrderStatus, Long> {
    List<TradeOrderStatus> findByTradeOrderId(Long tradeOrderId);
}
