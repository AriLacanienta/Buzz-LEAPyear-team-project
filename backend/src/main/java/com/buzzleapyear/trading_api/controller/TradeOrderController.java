package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.OrderStatusDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderDTO;
import com.buzzleapyear.trading_api.dto.TradeOrderResponseDTO;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.service.ProcessOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

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
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;

    public TradeOrderController(
            ProcessOrderService processOrderService,
            TradeOrderRepository tradeOrderRepository,
            AccountRepository accountRepository,
            InstrumentRepository instrumentRepository) {
        this.processOrderService = processOrderService;
        this.tradeOrderRepository = tradeOrderRepository;
        this.accountRepository = accountRepository;
        this.instrumentRepository = instrumentRepository;
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
    public ResponseEntity<TradeOrderResponseDTO> submitOrder(@Valid @RequestBody TradeOrderDTO orderDTO) {
        logger.info("Received POST /api/v1/tradeorders - side: {}, instrument: {}, qty: {}, price: {}",
            orderDTO.getSide(), orderDTO.getInstrumentId(), orderDTO.getQuantity(), orderDTO.getPrice());
        
        try {
            // Validate and load account
            Account account = accountRepository.findById(orderDTO.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + orderDTO.getAccountId()));
            
            // Validate and load instrument
            Instrument instrument = instrumentRepository.findById(orderDTO.getInstrumentId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + orderDTO.getInstrumentId()));
            
            // Create TradeOrder entity from DTO
            TradeOrder order = new TradeOrder();
            order.setAccount(account);
            order.setInstrument(instrument);
            order.setSide(orderDTO.getSide());
            order.setQuantity(orderDTO.getQuantity());
            order.setPrice(orderDTO.getPrice());
            order.setValue(orderDTO.getPrice().multiply(orderDTO.getQuantity()));
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
            order.addStatus(submittedStatus);
            tradeOrderRepository.save(order);
            logger.info("SUBMITTED status logged for order ID: {}", order.getId());
            
            // Submit for async processing
            processOrderService.submitOrder(order);
            
            // Return 202 Accepted with order ID
            TradeOrderResponseDTO response = new TradeOrderResponseDTO(
                order.getId(),
                TradeOrderStatus.OrderStatus.SUBMITTED,
                "Order submitted for processing"
            );
            
            logger.info("Returning 202 Accepted for order ID: {}", order.getId());
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
    public ResponseEntity<OrderStatusDTO> getOrderStatus(@PathVariable Long orderId) {
        logger.info("Received GET /api/v1/tradeorders/{}", orderId);
        
        try {
            // Fetch order
            TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElse(null);
            
            if (order == null) {
                logger.warn("Order not found: {}", orderId);
                return ResponseEntity.notFound().build();
            }
            
            // Get latest status
            TradeOrderStatus latestStatus = processOrderService.getOrderStatus(orderId);
            
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
}
