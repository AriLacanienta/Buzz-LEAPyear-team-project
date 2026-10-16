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
import java.time.LocalDateTime;

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

    @Query("SELECT o FROM TradeOrder o JOIN FETCH o.instrument WHERE o.account.id = :accountId AND o.side = :side "
        + "AND EXISTS (SELECT s FROM TradeOrderStatus s WHERE s.tradeOrder = o AND s.status = :status AND s.timeUpdated >= :since)")
        List<TradeOrder> findFilledOrdersSince(
            @Param("accountId") Long accountId,
            @Param("side") TradeOrder.OrderSide side,
            @Param("status") TradeOrderStatus.OrderStatus status,
            @Param("since") LocalDateTime since);
    
    List<TradeOrder> findByInstrumentId(Long instrumentId);
    List<TradeOrder> findByAccountIdOrderByOrderDateDesc(Long accountId);

    @Query(value = """
        SELECT o.id, a.id, a.accountName, u.firstName, u.lastName,
               i.instrumentSymbol, i.assetType, i.currencyCode,
               o.side, o.quantity, o.price, o.tradeValue, o.orderDate,
               s.status, s.timeUpdated, s.reasonText
        FROM TradeOrder o
        JOIN o.account a
        JOIN a.client c
        JOIN c.user u
        JOIN o.instrument i
        LEFT JOIN TradeOrderStatus s ON s.tradeOrder = o
            AND s.timeUpdated = (SELECT MAX(latest.timeUpdated)
                                 FROM TradeOrderStatus latest
                                 WHERE latest.tradeOrder = o)
                WHERE o.orderDate >= :fromDate AND o.orderDate < :toDate
                    AND (:search = '' OR
                             LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(u.username) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(a.accountName) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(i.instrumentSymbol) LIKE CONCAT('%', LOWER(:search), '%'))
        ORDER BY o.orderDate DESC, o.id DESC
        """,
                countQuery = """
                SELECT COUNT(o) FROM TradeOrder o
                JOIN o.account a
                JOIN a.client c
                JOIN c.user u
                JOIN o.instrument i
                WHERE o.orderDate >= :fromDate AND o.orderDate < :toDate
                    AND (:search = '' OR
                             LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(u.username) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(a.accountName) LIKE CONCAT('%', LOWER(:search), '%') OR
                             LOWER(i.instrumentSymbol) LIKE CONCAT('%', LOWER(:search), '%'))
                """)
    Page<Object[]> findAnalystTrades(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
                @Param("search") String search,
        Pageable pageable);
}
