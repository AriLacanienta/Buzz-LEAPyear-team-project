package com.buzzleapyear.trading_api.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzleapyear.trading_api.dto.WatchlistResponseDto;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.WatchlistItem;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.repository.WatchlistItemRepository;

@Service
public class WatchlistService {
    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final InstrumentService instrumentService;
    private final QuoteService quoteService;

    public WatchlistService(
        WatchlistItemRepository watchlistItemRepository,
        UserRepository userRepository,
        InstrumentService instrumentService,
        QuoteService quoteService
    ) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.instrumentService = instrumentService;
        this.quoteService = quoteService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponseDto> getWatchlist(String username) {
        User user = getUser(username);
        return watchlistItemRepository.findByUserOrderByCreatedAtAsc(user).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public WatchlistResponseDto add(String username, String symbol) {
        User user = getUser(username);
        Instrument instrument = getInstrument(symbol);

        WatchlistItem item = watchlistItemRepository.findByUserAndInstrument(user, instrument)
            .orElseGet(() -> {
                WatchlistItem newItem = new WatchlistItem();
                newItem.setUser(user);
                newItem.setInstrument(instrument);
                return watchlistItemRepository.save(newItem);
            });

        return toResponse(item);
    }

    @Transactional
    public void remove(String username, String symbol) {
        User user = getUser(username);
        Instrument instrument = getInstrument(symbol);
        watchlistItemRepository.findByUserAndInstrument(user, instrument)
            .ifPresent(watchlistItemRepository::delete);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
    }

    private Instrument getInstrument(String symbol) {
        String normalizedSymbol = Optional.ofNullable(symbol)
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .filter(value -> !value.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Symbol is required"));

        return instrumentService.getInstrumentBySymbol(normalizedSymbol)
            .orElseThrow(() -> new IllegalArgumentException("Instrument was not found: " + normalizedSymbol));
    }

    private WatchlistResponseDto toResponse(WatchlistItem item) {
        Instrument instrument = item.getInstrument();
        var quote = quoteService.getLatestQuoteBySymbol(instrument.getInstrumentSymbol());

        return new WatchlistResponseDto(
            item.getId(),
            instrument.getInstrumentSymbol(),
            instrument.getInstrumentName(),
            instrument.getAssetType().name(),
            instrument.getCurrencyCode(),
            quote.map(value -> value.price()).orElse(null),
            quote.map(value -> value.change()).orElse(null),
            quote.map(value -> value.changePercent()).orElse(null),
            quote.map(value -> value.timestamp()).orElse(null)
        );
    }
}
