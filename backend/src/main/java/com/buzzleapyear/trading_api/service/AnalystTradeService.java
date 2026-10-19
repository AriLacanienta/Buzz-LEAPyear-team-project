package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.AnalystTradeResponseDto;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AnalystTradeService {
    private final TradeOrderRepository tradeOrderRepository;

    public AnalystTradeService(TradeOrderRepository tradeOrderRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
    }

    public Page<AnalystTradeResponseDto> findTrades(LocalDate from, LocalDate to, String search, Pageable pageable) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date must be on or after the start date");
        }

        var rows = tradeOrderRepository.findAnalystTrades(
            start.atStartOfDay(), end.plusDays(1).atStartOfDay(), search == null ? "" : search.trim(), pageable);
        var content = rows.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(content, pageable, rows.getTotalElements());
    }

    private AnalystTradeResponseDto toDto(Object[] row) {
        String clientName = ((String) row[3] + " " + (String) row[4]).trim();
        return new AnalystTradeResponseDto(
            (Long) row[0], (Long) row[1], (String) row[2], clientName,
            (String) row[5], (com.buzzleapyear.trading_api.entity.Instrument.AssetType) row[6],
            (String) row[7], (com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide) row[8],
            (java.math.BigDecimal) row[9], (java.math.BigDecimal) row[10],
            (java.math.BigDecimal) row[11], (LocalDateTime) row[12],
            (com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus) row[13],
            (LocalDateTime) row[14], (String) row[15]
        );
    }
}