package com.buzzleapyear.trading_api.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;  
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.List;
import com.buzzleapyear.trading_api.dto.TradeActivityReportDto;
import com.buzzleapyear.trading_api.repository.TradeAnalyticsRepository;

@RestController
@RequestMapping("/api/v1/analyst")
public class TradeActivityReportController {
    private final TradeAnalyticsRepository reportRepository;

    public TradeActivityReportController(TradeAnalyticsRepository analyticsRepository) {
        this.reportRepository = analyticsRepository;
    }

    @GetMapping("/filled-trades")
    public ResponseEntity<List<TradeActivityReportDto>> getTradeActivity(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String instrumentSymbol
    ) {
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reportRepository.findFilledTrades(from, to, instrumentSymbol));
    }

    @GetMapping("/trade-events")
    public ResponseEntity<List<TradeActivityReportDto>> getAllTrades(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String instrumentSymbol
    ) {
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reportRepository.findAllTrades(from, to, instrumentSymbol));
    }
}
