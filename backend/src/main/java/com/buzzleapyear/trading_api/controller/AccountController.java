package com.buzzleapyear.trading_api.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import com.buzzleapyear.trading_api.dto.AccountValueResponseDto;
import com.buzzleapyear.trading_api.dto.CurrentAccountResponseDto;
import com.buzzleapyear.trading_api.dto.RecentTradeOrderResponseDto;
import com.buzzleapyear.trading_api.service.AccountService;

@RestController
@RequestMapping("/api/v1/accounts") 
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentAccountResponseDto> getCurrentAccount(Authentication authentication) {
        return ResponseEntity.ok(accountService.getCurrentAccount(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountValueResponseDto> getAccountValue(@PathVariable String id, Authentication authentication) {
        return accountService.getAccountValue(id, authentication.getName())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tradeorders")
    public ResponseEntity<List<RecentTradeOrderResponseDto>> getRecentTradeOrders(
        @PathVariable Long accountId,
        @RequestParam(defaultValue = "10") int limit,
        Authentication authentication) {
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest().build();
            }

            return accountService.getRecentTradeOrders(accountId, authentication.getName(), limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        }
}
