package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Dispute;
import com.buzzleapyear.trading_api.entity.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {
    boolean existsByTradeOrderIdAndStatus(Long tradeOrderId, DisputeStatus status);
    Optional<Dispute> findByIdAndTradeOrderAccountClientUserUsername(Long id, String username);
    Page<Dispute> findByTradeOrderAccountClientUserUsername(String username, Pageable pageable);
}
