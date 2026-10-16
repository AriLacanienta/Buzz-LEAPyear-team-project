package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.ManagerTradeLogResponseDTO;
import com.buzzleapyear.trading_api.service.ManagerTradeLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/manager/tradeorders")
public class ManagerTradeLogController {
    private final ManagerTradeLogService managerTradeLogService;

    public ManagerTradeLogController(ManagerTradeLogService managerTradeLogService) {
        this.managerTradeLogService = managerTradeLogService;
    }

    @GetMapping
    public ResponseEntity<Page<ManagerTradeLogResponseDTO>> getTradeLog(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"))
        );
        return ResponseEntity.ok(managerTradeLogService.getTradeLog(pageable));
    }
}
