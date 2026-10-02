package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.HoldingResponseDto;
import com.buzzleapyear.trading_api.service.HoldingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@RestController
@RequestMapping("api/v1/accounts")
public class HoldingController {
    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<HoldingResponseDto>> getAccountHoldings(@PathVariable Long accountId) {
        List<HoldingResponseDto> holdings = holdingService.getHoldingsByAccountId(accountId);
        return ResponseEntity.ok(holdings);
    }
}