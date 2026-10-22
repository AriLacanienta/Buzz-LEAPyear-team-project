package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.dto.DisputeResponse;
import com.buzzleapyear.trading_api.entity.Dispute;
import com.buzzleapyear.trading_api.entity.DisputeStatus;
import com.buzzleapyear.trading_api.entity.DisputeSeverity;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.repository.DisputeRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class DisputeService {
    private static final BigDecimal CRITICAL_TRADE_VALUE = new BigDecimal("10000.00");
    private final DisputeRepository disputeRepository;
    private final TradeOrderRepository tradeOrderRepository;

    public DisputeService(DisputeRepository disputeRepository, TradeOrderRepository tradeOrderRepository) {
        this.disputeRepository = disputeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
    }

    @Transactional
    public DisputeResponse create(CreateDisputeRequest request, String username) {
        if (request == null || request.tradeOrderId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Trade order ID is required");
        }
        TradeOrder order = tradeOrderRepository.findById(request.tradeOrderId())
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Trade order not found"));
        if (!isOwnedBy(order, username)) {
            throw error(HttpStatus.FORBIDDEN, "Only the account owner may create a dispute");
        }
        if (order.getStatuses().stream().noneMatch(status -> status.getStatus() == OrderStatus.FILLED)) {
            throw error(HttpStatus.CONFLICT, "Only successfully executed orders may be disputed");
        }
        if (disputeRepository.existsByTradeOrderIdAndStatus(order.getId(), DisputeStatus.OPEN)) {
            throw error(HttpStatus.CONFLICT, "An open dispute already exists for this trade order");
        }
        Dispute dispute = new Dispute();
        dispute.setTradeOrder(order);
        dispute.setMessage(normalizeMessage(request.message()));
        return toResponse(disputeRepository.save(dispute));
    }

    @Transactional(readOnly = true)
    public DisputeResponse get(Long id, String username, boolean manager) {
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dispute not found"));
        if (!manager && !isOwnedBy(dispute.getTradeOrder(), username)) {
            throw error(HttpStatus.FORBIDDEN, "You may not view this dispute");
        }
        return toResponse(dispute);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> search(String username, boolean manager, Long accountId,
                                        LocalDateTime fromDate, LocalDateTime toDate,
                                        BigDecimal minTradeValue, BigDecimal maxTradeValue,
                                        Pageable pageable) {
        Specification<Dispute> specification = (root, query, cb) -> cb.conjunction();
        // Non-managers may only query their own disputes
        if (!manager) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("tradeOrder").get("account").get("client").get("user").get("username"), username));
        }
        // Match accountId, if provided
        if (accountId != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("tradeOrder").get("account").get("id"), accountId));
        }
        
        if (fromDate != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        if (minTradeValue != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("tradeOrder").get("tradeValue"), minTradeValue));
        }
        if (maxTradeValue != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("tradeOrder").get("tradeValue"), maxTradeValue));
        }
        return disputeRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional
    public DisputeResponse resolve(Long id, Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
            .noneMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()))) {
            throw error(HttpStatus.FORBIDDEN, "Only managers may resolve disputes");
        }
        Dispute dispute = disputeRepository.findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dispute not found"));
        if (isOwnedBy(dispute.getTradeOrder(), authentication.getName())) {
            throw error(HttpStatus.FORBIDDEN, "Managers may not resolve disputes for their own accounts");
        }
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            return toResponse(dispute);
        }
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedAt(LocalDateTime.now());
        return toResponse(disputeRepository.save(dispute));
    }

    private boolean isOwnedBy(TradeOrder order, String username) {
        return order != null
            && order.getAccount() != null
            && order.getAccount().getClient() != null
            && order.getAccount().getClient().getUser() != null
            && username != null
            && username.equals(order.getAccount().getClient().getUser().getUsername());
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private DisputeResponse toResponse(Dispute dispute) {
        TradeOrder order = dispute.getTradeOrder();
        return new DisputeResponse(dispute.getId(), order.getId(), order.getAccount().getId(),
            order.getValue(), dispute.getMessage(), dispute.getStatus(), severityOf(order),
            dispute.getCreatedAt(), dispute.getResolvedAt());
    }

    private DisputeSeverity severityOf(TradeOrder order) {
        if (order.getQuantity() == null || order.getPrice() == null
            || order.getQuantity().signum() <= 0 || order.getPrice().signum() <= 0) {
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "Order execution quantity and price must be positive");
        }
        BigDecimal executionValue = order.getQuantity().multiply(order.getPrice())
            .setScale(2, RoundingMode.HALF_UP);
        return executionValue.compareTo(CRITICAL_TRADE_VALUE) >= 0
            ? DisputeSeverity.CRITICAL : DisputeSeverity.NORMAL;
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
