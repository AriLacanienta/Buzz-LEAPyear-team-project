package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByAccountId(Long accountId);
    
    @Query("SELECT DISTINCT to FROM TradeOrder to LEFT JOIN FETCH to.instrument WHERE to.account.id = ?1 ORDER BY to.orderDate DESC")
    Page<TradeOrder> findByAccountIdOrderByOrderDateDesc(Long accountId, Pageable pageable);
    
    List<TradeOrder> findByInstrumentId(Long instrumentId);
}
