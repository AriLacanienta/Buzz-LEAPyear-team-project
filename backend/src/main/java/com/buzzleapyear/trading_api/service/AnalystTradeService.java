package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.AnalystTradeResponseDto;
import com.buzzleapyear.trading_api.repository.TradeAnalyticsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AnalystTradeService {
    private final TradeAnalyticsRepository tradeAnalyticsRepository;

    public AnalystTradeService(TradeAnalyticsRepository tradeAnalyticsRepository) {
        this.tradeAnalyticsRepository = tradeAnalyticsRepository;
    }

    public Page<AnalystTradeResponseDto> findTrades(LocalDate from, LocalDate to, String search, Pageable pageable) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date must be on or after the start date");
        }

        return tradeAnalyticsRepository.findAnalystTrades(start, end, search, pageable);
    }
}