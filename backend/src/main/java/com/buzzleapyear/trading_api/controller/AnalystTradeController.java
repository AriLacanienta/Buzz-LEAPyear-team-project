package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.AnalystTradeResponseDto;
import com.buzzleapyear.trading_api.service.AnalystTradeService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyst/trades")
public class AnalystTradeController {
    private final AnalystTradeService analystTradeService;

    public AnalystTradeController(AnalystTradeService analystTradeService) {
        this.analystTradeService = analystTradeService;
    }

    @GetMapping
    public ResponseEntity<Page<AnalystTradeResponseDto>> getTrades(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100 || (from != null && to != null && to.isBefore(from))) {
            return ResponseEntity.badRequest().build();
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        return ResponseEntity.ok(analystTradeService.findTrades(from, to, search, pageable));
    }
}