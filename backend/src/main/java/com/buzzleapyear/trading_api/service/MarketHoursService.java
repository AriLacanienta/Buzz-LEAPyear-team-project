package com.buzzleapyear.trading_api.service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

@Service
public class MarketHoursService {
    private record Hours(String marketName, ZoneId zone, LocalTime open, LocalTime close, String label) {}

    private static final Map<TradingSession, Hours> SESSION_HOURS = Map.of(
        TradingSession.US_MARKET, new Hours("US", ZoneId.of("America/New_York"), LocalTime.of(9, 30), LocalTime.of(16, 0), "9:30 AM - 4:00 PM ET, Mon-Fri"),
        TradingSession.UK_MARKET, new Hours("UK", ZoneId.of("Europe/London"), LocalTime.of(8, 0), LocalTime.of(16, 30), "8:00 AM - 4:30 PM BST, Mon-Fri"),
        TradingSession.INDIA_MARKET, new Hours("India", ZoneId.of("Asia/Kolkata"), LocalTime.of(9, 15), LocalTime.of(15, 30), "9:15 AM - 3:30 PM IST, Mon-Fri")
    );

    private final Clock clock;
    private final boolean enforced;

    @Autowired
    public MarketHoursService(@Value("${MARKET_HOURS_ENFORCED:true}") boolean enforced) {
        this(Clock.systemUTC(), enforced);
    }

    MarketHoursService(Clock clock, boolean enforced) {
        this.clock = clock;
        this.enforced = enforced;
    }

    public boolean isOpen(TradingSession session) {
        if (!enforced || session == null || session == TradingSession.ALWAYS_OPEN) {
            return true;
        }
        Hours hours = SESSION_HOURS.get(session);
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(hours.zone()));
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(hours.open()) && time.isBefore(hours.close());
    }

    public String closedReason(TradingSession session) {
        Hours hours = SESSION_HOURS.get(session);
        if (hours == null) {
            return "Market is closed";
        }
        return String.format("%s market is closed. Trading hours: %s", hours.marketName(), hours.label());
    }
}