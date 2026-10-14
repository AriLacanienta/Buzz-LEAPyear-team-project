package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.HoldingResponseDto;
import com.buzzleapyear.trading_api.service.HoldingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.buzzleapyear.trading_api.service.AccountService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holdings")
public class HoldingController {
    private final HoldingService holdingService;
    private final AccountService accountService;

    public HoldingController(HoldingService holdingService, AccountService accountService) {
        this.holdingService = holdingService;
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<HoldingResponseDto>> getAccountHoldings(@PathVariable Long accountId, Authentication authentication) {
        if (accountService.findAccountByIdForUser(accountId, authentication.getName()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<HoldingResponseDto> holdings = holdingService.getHoldingsByAccountId(accountId);
        return ResponseEntity.ok(holdings);
    }
}