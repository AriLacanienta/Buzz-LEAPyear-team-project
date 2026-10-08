package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByAccountId(Long accountId);

    @EntityGraph(attributePaths = {"account", "account.holdings", "account.holdings.instrument", "instrument"})
    @Query("SELECT DISTINCT o FROM TradeOrder o WHERE o.id = :orderId")
    Optional<TradeOrder> findByIdForProcessing(@Param("orderId") Long orderId);
    
    @Query(
        value = "SELECT o FROM TradeOrder o LEFT JOIN FETCH o.instrument WHERE o.account.id = ?1 ORDER BY o.orderDate DESC, o.id DESC",
        countQuery = "SELECT COUNT(o) FROM TradeOrder o WHERE o.account.id = ?1"
    )
    Page<TradeOrder> findByAccountIdOrderByOrderDateDesc(Long accountId, Pageable pageable);
    
    List<TradeOrder> findByInstrumentId(Long instrumentId);
    List<TradeOrder> findByAccountIdOrderByOrderDateDesc(Long accountId);
}
