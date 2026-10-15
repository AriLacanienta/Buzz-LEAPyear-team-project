package com.buzzleapyear.trading_api.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;
import com.buzzleapyear.trading_api.dto.ManagerTradeLogResponseDTO;
import com.buzzleapyear.trading_api.service.ManagerTradeLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/v1/manager/tradeorders?page=0&size=25
@RestController
@RequestMapping("/api/v1/manager/tradeorders")
public class ManagerTradeLogController {
    private final ManagerTradeLogService service;

    public ManagerTradeLogController(ManagerTradeLogService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ManagerTradeLogResponseDTO> getTradeLog(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination values");
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("orderDate"),
                Sort.Order.desc("id")
            )
        );

        return service.getTradeLog(pageable);
    }
}