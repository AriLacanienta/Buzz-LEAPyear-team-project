package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import java.time.LocalDateTime;
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

    @EntityGraph(attributePaths = {"account", "account.client", "account.client.user", "instrument"})
    @Query("SELECT o FROM TradeOrder o ORDER BY o.orderDate DESC, o.id DESC")
    Page<TradeOrder> findAllForManager(Pageable pageable);
    @EntityGraph(attributePaths = {"account", "account.client", "account.client.user", "instrument"})
    @Query("SELECT o FROM TradeOrder o WHERE o.id = :orderId")
    Optional<TradeOrder> findForManagerById(@Param("orderId") Long orderId);
    @Query("SELECT o FROM TradeOrder o JOIN FETCH o.instrument WHERE o.account.id = :accountId AND o.side = :side "
        + "AND EXISTS (SELECT s FROM TradeOrderStatus s WHERE s.tradeOrder = o AND s.status = :status AND s.timeUpdated >= :since)")
        List<TradeOrder> findFilledOrdersSince(
            @Param("accountId") Long accountId,
            @Param("side") TradeOrder.OrderSide side,
            @Param("status") TradeOrderStatus.OrderStatus status,
            @Param("since") LocalDateTime since);
    
    List<TradeOrder> findByInstrumentId(Long instrumentId);
    List<TradeOrder> findByAccountIdOrderByOrderDateDesc(Long accountId);
}
