package com.buzzleapyear.trading_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.dto.WatchlistRequestDto;
import com.buzzleapyear.trading_api.dto.WatchlistResponseDto;
import com.buzzleapyear.trading_api.service.WatchlistService;

@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ResponseEntity<List<WatchlistResponseDto>> getWatchlist(Authentication authentication) {
        return ResponseEntity.ok(watchlistService.getWatchlist(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<WatchlistResponseDto> addToWatchlist(
        @RequestBody WatchlistRequestDto request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(watchlistService.add(authentication.getName(), request.symbol()));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(
        @PathVariable String symbol,
        Authentication authentication
    ) {
        watchlistService.remove(authentication.getName(), symbol);
        return ResponseEntity.noContent().build();
    }
}
