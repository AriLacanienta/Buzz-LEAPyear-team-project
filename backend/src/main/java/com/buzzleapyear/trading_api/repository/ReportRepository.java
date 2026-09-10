package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReportName(String reportName);
    List<Report> findByUserId(Long userId);
}
