package com.buzzleapyear.trading_api.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.dto.OrderStatusDTO;
import com.buzzleapyear.trading_api.dto.RecentOrderResponseDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderResponseDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderPreviewRequestDto;
import com.buzzleapyear.trading_api.dto.TradeOrderPreviewResponseDto;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.service.ProcessOrderService;
import com.buzzleapyear.trading_api.service.TradeOrderService;
import com.buzzleapyear.trading_api.service.AccountService;
import com.buzzleapyear.trading_api.service.OrderProcessor;

import jakarta.validation.Valid;

/**
 * TradeOrderController
 * REST API endpoints for trade order management
 * 
 * @author Ari Lacanienta
 */
@RestController
@RequestMapping("api/v1/tradeorders")
public class TradeOrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeOrderController.class);
    
    private final ProcessOrderService processOrderService;
    private final TradeOrderRepository tradeOrderRepository;
    private final TradeOrderStatusRepository tradeOrderStatusRepository;
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final TradeOrderService tradeOrderService;
    private final AccountService accountService;
    private final OrderProcessor orderProcessor;

    public TradeOrderController(
            ProcessOrderService processOrderService,
            TradeOrderRepository tradeOrderRepository,
            TradeOrderStatusRepository tradeOrderStatusRepository,
            AccountRepository accountRepository,
            InstrumentRepository instrumentRepository,
            TradeOrderService tradeOrderService,
            AccountService accountService,
            OrderProcessor orderProcessor) {
        this.processOrderService = processOrderService;
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderStatusRepository = tradeOrderStatusRepository;
        this.accountRepository = accountRepository;
        this.instrumentRepository = instrumentRepository;
        this.tradeOrderService = tradeOrderService;
        this.accountService = accountService;
        this.orderProcessor = orderProcessor;
    }

    /*
    * Estimate a trade at current live price without being placed
    */
    @PostMapping("/preview")
    public ResponseEntity<TradeOrderPreviewResponseDto> previewOrder(@Valid @RequestBody TradeOrderPreviewRequestDto request, Authentication authentication) {
        Account account = accountService.findAccountByIdForUser(request.accountId(), authentication.getName()).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        Instrument instrument = instrumentRepository.findByInstrumentSymbol(request.instrumentSymbol()).orElse(null);
        if (instrument == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderProcessor.previewOrder(account, instrument, request.side(), request.quantity()));
    }

    /**
     * Submit a new trade order
     * Returns immediately (HTTP 202 Accepted) with order ID
     * Processing happens asynchronously in background
     * 
     * @param orderDTO the order details
     * @return 202 Accepted with TradeOrderResponseDTO containing orderId
     */
    @PostMapping
    public ResponseEntity<TradeOrderResponseDTO> submitOrder(@Valid @RequestBody TradeOrderDTO orderDTO, Authentication authentication) {
        logger.info("Received POST /api/v1/tradeorders - side: {}, instrument: {}, qty: {}, price: {}",
            orderDTO.side(), orderDTO.instrumentSymbol(), orderDTO.quantity(), orderDTO.price());
        
        try {
            // Validate and load account
            Account account = accountService.findAccountByIdForUser(orderDTO.accountId(), authentication.getName()).orElse(null);
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Validate and load instrument
            Instrument instrument = instrumentRepository.findByInstrumentSymbol(orderDTO.instrumentSymbol())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + orderDTO.instrumentSymbol()));
            
            // Create TradeOrder entity from DTO
            TradeOrder order = new TradeOrder();
            order.setAccount(account);
            order.setInstrument(instrument);
            order.setSide(orderDTO.side());
            order.setQuantity(orderDTO.quantity());
            order.setPrice(orderDTO.price());
            order.setValue(orderDTO.price().multiply(orderDTO.quantity()));
            order.setOrderDate(LocalDateTime.now());
            
            // Save order to database (generates ID)
            order = tradeOrderRepository.save(order);
            logger.info("Order persisted with ID: {}", order.getId());
            
            // Create initial SUBMITTED status
            TradeOrderStatus submittedStatus = new TradeOrderStatus();
            submittedStatus.setTradeOrder(order);
            submittedStatus.setStatus(TradeOrderStatus.OrderStatus.SUBMITTED);
            submittedStatus.setTimeUpdated(LocalDateTime.now());
            submittedStatus.setReasonText(null);
            tradeOrderStatusRepository.save(submittedStatus);
            logger.info("SUBMITTED status logged for order ID: {}", order.getId());
            
            // Submit for processing
            processOrderService.submitOrder(order.getId());

            logger.info("Return 202 Created for order ID: {}", order.getId());

            TradeOrderResponseDTO response = new TradeOrderResponseDTO(
                order.getId(),
                TradeOrderStatus.OrderStatus.SUBMITTED,
                "Order submitted for processing"
            );
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error processing order submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the status of an existing trade order
     * 
     * @param orderId the ID of the order to query
     * @return 200 OK with OrderStatusDTO if found, 404 Not Found otherwise
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusDTO> getOrderStatus(@PathVariable Long orderId, Authentication authentication) {
        logger.info("Received GET /api/v1/tradeorders/{}", orderId);
        
        try {
            // Fetch order
            TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElse(null);
            
            if (order == null) {
                logger.warn("Order not found: {}", orderId);
                return ResponseEntity.notFound().build();
            }

            if (accountService.findAccountByIdForUser(order.getAccount().getId(), authentication.getName()).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Get latest status
            TradeOrderStatus latestStatus = tradeOrderStatusRepository.getLatestStatusById(orderId);
            
            if (latestStatus == null) {
                logger.warn("No status found for order: {}", orderId);
                return ResponseEntity.notFound().build();
            }
            
            // Build response DTO
            OrderStatusDTO response = new OrderStatusDTO(
                order.getId(),
                order.getAccount().getId(),
                latestStatus.getStatus(),
                latestStatus.getReasonText(),
                latestStatus.getTimeUpdated()
            );
            
            logger.info("Returning 200 OK for order ID: {} with status: {}", orderId, latestStatus.getStatus());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving order status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/accounts/{accountId}/orders")
    public ResponseEntity<List<RecentOrderResponseDTO>> getRecentOrders(
        @PathVariable Long accountId,
        @RequestParam(defaultValue = "20") int limit) {
        
        List<RecentOrderResponseDTO> orders = tradeOrderService.getRecentOrdersByAccountId(accountId, limit);
        return ResponseEntity.ok(orders);
    }
}
