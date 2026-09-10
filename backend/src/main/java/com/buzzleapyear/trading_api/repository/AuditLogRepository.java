package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByReportId(Long reportId);
    List<AuditLog> findByClientId(Long clientId);
    List<AuditLog> findByAccountId(Long accountId);
}
