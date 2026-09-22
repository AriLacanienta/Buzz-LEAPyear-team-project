package com.buzzleapyear.trading_api.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.service.InstrumentService;
import com.buzzleapyear.trading_api.repository.QuoteRepository;
import com.buzzleapyear.trading_api.entity.Quote;
import com.buzzleapyear.trading_api.dto.QuoteResponseDto;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/*
    INSERT INTO instruments (instrument_name, instrument_symbol, asset_type, currency_code) VALUES ('Apple Inc.', 'AAPL', 'EQUITY', 'USD');
    INSERT INTO instruments (instrument_name, instrument_symbol, asset_type, currency_code) VALUES ('Microsoft Corporation', 'MSFT', 'EQUITY', 'USD');
    INSERT INTO instruments (instrument_name, instrument_symbol, asset_type, currency_code) VALUES ('Alphabet Inc.', 'GOOGL', 'EQUITY', 'USD');
    INSERT INTO instruments (instrument_name, instrument_symbol, asset_type, currency_code) VALUES ('Tesla Inc.', 'TSLA', 'EQUITY', 'USD');

    INSERT INTO quotes (instrument_id, price, high_price_of_day, low_price_of_day, open_price_of_day, previous_close_price_of_day, change_amount, change_percent, volume, market_cap, timestamp) VALUES (1, 175.50, 176.25, 174.80, 175.00, 174.25, 1.25, 0.72, 52500000.00, 2750000000000.00, NOW());
    INSERT INTO quotes (instrument_id, price, high_price_of_day, low_price_of_day, open_price_of_day, previous_close_price_of_day, change_amount, change_percent, volume, market_cap, timestamp) VALUES (2, 425.50, 428.00, 424.25, 426.00, 424.50, 1.00, 0.24, 45000000.00, 2850000000000.00, NOW());
    INSERT INTO quotes (instrument_id, price, high_price_of_day, low_price_of_day, open_price_of_day, previous_close_price_of_day, change_amount, change_percent, volume, market_cap, timestamp) VALUES (3, 182.50, 185.00, 181.75, 183.00, 181.25, 1.25, 0.69, 38000000.00, 1200000000000.00, NOW());
    INSERT INTO quotes (instrument_id, price, high_price_of_day, low_price_of_day, open_price_of_day, previous_close_price_of_day, change_amount, change_percent, volume, market_cap, timestamp) VALUES (4, 245.75, 248.50, 242.00, 244.00, 241.50, 4.25, 1.76, 52000000.00, 780000000000.00, NOW());

    Future work:
    Periodically save quotes data to the database in intervals (e.g., every 5 minutes)
    Implement market rules - trading hours

*/

@Service
public class QuoteService {
    private final double MIN_VOLATILITY;
    private final double MAX_VOLATILITY;

    private static class InstrumentQuoteState {
        BigDecimal currentPrice;
        double volatility;
        BigDecimal highPrice;
        BigDecimal lowPrice;
        BigDecimal openPrice;
        BigDecimal previousClosePrice;
        BigDecimal change;
        BigDecimal changePercent;
        long volume;
        BigDecimal marketCap;
        LocalDateTime timestamp;
    }

    private final InstrumentService instrumentService;
    private final QuoteRepository quoteRepository;
    private final Map<Long, InstrumentQuoteState> latestQuotes = new ConcurrentHashMap<>();

    public QuoteService(InstrumentService instrumentService, QuoteRepository quoteRepository, @Value("${QUOTE_MIN_VOLATILITY:0.0}") double minVolatility, @Value("${QUOTE_MAX_VOLATILITY:0.0}") double maxVolatility) {
        this.instrumentService = instrumentService;
        this.quoteRepository = quoteRepository;
        this.MIN_VOLATILITY = minVolatility;
        this.MAX_VOLATILITY = maxVolatility;
    }
    
    /*
     * Initializes the latest quotes for all instruments by fetching the most recent quote from the repository.
     * If no quote is found for an instrument, it will not be added to the latestQuotes map.
     */
    @PostConstruct
    public void init() {
        try {
            for (Instrument instrument : instrumentService.getAllInstruments()) {
            InstrumentQuoteState state = new InstrumentQuoteState();

            Quote quote = quoteRepository
            .findTopByInstrumentIdOrderByTimestampDesc(instrument.getInstrumentId())
            .orElse(null);

            if (quote != null) {
                state.currentPrice = quote.getPrice();
                state.volatility = randomNum(MIN_VOLATILITY, MAX_VOLATILITY).doubleValue();
                state.highPrice = quote.getHighPriceOfDay();
                state.lowPrice = quote.getLowPriceOfDay();
                state.openPrice = quote.getOpenPriceOfDay();
                state.previousClosePrice = quote.getPreviousClosePriceOfDay();
                state.change = quote.getChange();
                state.changePercent = quote.getChangePercent();
                state.volume = quote.getVolume();
                state.marketCap = quote.getMarketCap();
                state.timestamp = quote.getTimestamp();

                latestQuotes.put(instrument.getInstrumentId(), state);
                System.out.println("QUOTE for instrument " + instrument.getInstrumentId() + ": " + quote.getPrice());
            }
        }
        } catch (Exception e) {
            System.err.println("Error initializing quotes: " + e.getMessage());
        }
    }

    public void reinitializeQuotes() {
        latestQuotes.clear();
        init();
    }

    /*
     * Updates the quotes for all instruments at a fixed rate defined by the quoteFixedRate property.
     * If no quotes are loaded yet, the update will be skipped.
     */
    @Scheduled(fixedRateString = "${quoteFixedRate}")
    public void updateQuotes() {
        if (latestQuotes.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, InstrumentQuoteState> prevEntry : latestQuotes.entrySet()) {
            BigDecimal prevPrice = prevEntry.getValue().currentPrice;
            Long instrumentId = prevEntry.getKey();

            if (prevPrice == null) continue;

            double volatility = prevEntry.getValue().volatility; // How big a move would be

            BigDecimal newPrice = generateNextPrice(prevPrice, volatility);
            InstrumentQuoteState prevState = prevEntry.getValue();
            InstrumentQuoteState newState = new InstrumentQuoteState();

            newState.currentPrice = newPrice;
            newState.volatility = volatility;
            newState.highPrice = newPrice.max(prevState.highPrice);
            newState.lowPrice = newPrice.min(prevState.lowPrice);
            newState.openPrice = prevState.openPrice;
            newState.previousClosePrice = prevState.previousClosePrice;
            newState.change = newPrice.subtract(prevState.previousClosePrice);
            newState.changePercent = newPrice.subtract(prevState.previousClosePrice)
            .divide(prevState.previousClosePrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
            newState.volume = prevState.volume + ThreadLocalRandom.current().nextLong(1000);
            newState.marketCap = prevState.marketCap;
            newState.timestamp = java.time.LocalDateTime.now();

            latestQuotes.put(instrumentId, newState);
            System.out.println(
                "QUOTE:" + instrumentId + " - PRICE: $" + newPrice + " - HIGH: $" + newState.highPrice + " - LOW: $" 
                + newState.lowPrice + " - CHANGE: $" + newState.change + " - CHANGE%: " + newState.changePercent + "% - VOLUME: " + newState.volume
            );
        }
        System.out.println("----------------------------------------------");
    }

    public List<QuoteResponseDto> getAllLatestQuotes() {
        List<Instrument> instruments = instrumentService.getAllInstruments();
        List<QuoteResponseDto> responses = new ArrayList<>();

        for (Instrument instrument : instruments) {
            try {
                long instrumentId = instrument.getInstrumentId();

                responses.add(new QuoteResponseDto(
                    instrument.getInstrumentSymbol(),
                    latestQuotes.get(instrumentId).currentPrice,
                    latestQuotes.get(instrumentId).highPrice,
                    latestQuotes.get(instrumentId).lowPrice,
                    latestQuotes.get(instrumentId).openPrice,
                    latestQuotes.get(instrumentId).previousClosePrice,
                    latestQuotes.get(instrumentId).change,
                    latestQuotes.get(instrumentId).changePercent,
                    latestQuotes.get(instrumentId).volume,
                    latestQuotes.get(instrumentId).marketCap,
                    latestQuotes.get(instrumentId).timestamp
                ));
            } catch (Exception e) {
                System.err.println("Error fetching quote for instrument " + instrument.getInstrumentId() + ": " + e.getMessage());
            }
        }
        responses.sort(Comparator.comparing(QuoteResponseDto::symbol));

        return responses;
    }

    public Optional<QuoteResponseDto> getLatestQuoteBySymbol(String symbol) {
        Optional<Instrument> instrument = instrumentService.getInstrumentBySymbol(symbol);
        if (instrument.isEmpty()) {
            return Optional.empty();
        }

        Long instrumentId = instrument.get().getInstrumentId();
        InstrumentQuoteState state = latestQuotes.get(instrumentId);

        if (state == null || state.currentPrice == null) {
            return Optional.empty();
        }

        return Optional.of(new QuoteResponseDto(
            symbol,
            state.currentPrice, 
            state.highPrice, 
            state.lowPrice, 
            state.openPrice, 
            state.previousClosePrice, 
            state.change,
            state.changePercent, 
            state.volume,
            state.marketCap,
            state.timestamp
        ));
    }

    private BigDecimal generateNextPrice(BigDecimal prevPrice, double volatility) {
        double shock = ThreadLocalRandom.current().nextGaussian(); // Random value ranging *mostly* from -3 to 3, but generally closer to 0
        double changePercent = volatility * shock; // Scale the random value

        BigDecimal change = prevPrice.multiply(BigDecimal.valueOf(changePercent));
        BigDecimal newPrice = prevPrice.add(change).setScale(2, RoundingMode.HALF_UP);
        return newPrice;
    }

    private BigDecimal randomNum(double min, double max) {
        double randomValue = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(randomValue).setScale(3, RoundingMode.HALF_UP);
    }
}