package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.dto.DisputeResponse;
import com.buzzleapyear.trading_api.dto.ManagerDashboardSummary;
import com.buzzleapyear.trading_api.service.DisputeService;
import com.buzzleapyear.trading_api.service.ManagerDashboardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/dispute")
public class DisputeController {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "status", "tradeValue");
    private final DisputeService disputeService;
    private final ManagerDashboardService dashboardService;

    public DisputeController(DisputeService disputeService, ManagerDashboardService dashboardService) {
        this.disputeService = disputeService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ManagerDashboardSummary summary(Authentication authentication) {
        return dashboardService.summary(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<DisputeResponse> create(@Valid @RequestBody CreateDisputeRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(201).body(disputeService.create(request, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisputeResponse> get(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(disputeService.get(id, authentication.getName(), isManager(authentication)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DisputeResponse>> search(
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection,
        @RequestParam(required = false) Long accountId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @RequestParam(required = false) BigDecimal minTradeValue,
        @RequestParam(required = false) BigDecimal maxTradeValue,
        Authentication authentication) {
        if (page < 0 || size < 1 || size > 100 || !SORT_FIELDS.contains(sortBy)
            || (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc"))
            || (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            || (minTradeValue != null && minTradeValue.signum() < 0)
            || (maxTradeValue != null && maxTradeValue.signum() < 0)
            || (minTradeValue != null && maxTradeValue != null && minTradeValue.compareTo(maxTradeValue) > 0)) {
            return ResponseEntity.badRequest().build();
        }
        String path = sortBy.equals("tradeValue") ? "tradeOrder.tradeValue" : sortBy;
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, path).and(Sort.by("id")));
        return ResponseEntity.ok(disputeService.search(authentication.getName(), isManager(authentication),
            accountId, fromDate, toDate, minTradeValue, maxTradeValue, pageable));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<DisputeResponse> resolve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(disputeService.resolve(id, authentication));
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }
}
