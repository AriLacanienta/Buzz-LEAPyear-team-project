package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.dto.DisputeResponse;
import com.buzzleapyear.trading_api.entity.Dispute;
import com.buzzleapyear.trading_api.entity.DisputeStatus;
import com.buzzleapyear.trading_api.entity.TradeOrder;
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
import java.time.LocalDateTime;

@Service
public class DisputeService {
    private final DisputeRepository disputeRepository;
    private final TradeOrderRepository tradeOrderRepository;

    public DisputeService(DisputeRepository disputeRepository, TradeOrderRepository tradeOrderRepository) {
        this.disputeRepository = disputeRepository;
        this.tradeOrderRepository = tradeOrderRepository;
    }

    @Transactional
    public DisputeResponse create(CreateDisputeRequest request, String username) {
        TradeOrder order = tradeOrderRepository.findById(request.tradeOrderId())
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Trade order not found"));
        if (order.getAccount().getClient().getUser() == null
            || !username.equals(order.getAccount().getClient().getUser().getUsername())) {
            throw error(HttpStatus.FORBIDDEN, "Only the account owner may create a dispute");
        }
        if (disputeRepository.existsByTradeOrderIdAndStatus(order.getId(), DisputeStatus.OPEN)) {
            throw error(HttpStatus.CONFLICT, "An open dispute already exists for this trade order");
        }
        Dispute dispute = new Dispute();
        dispute.setTradeOrder(order);
        dispute.setMessage(request.message());
        return toResponse(disputeRepository.save(dispute));
    }

    @Transactional(readOnly = true)
    public DisputeResponse get(Long id, String username, boolean manager) {
        Dispute dispute = manager
            ? disputeRepository.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dispute not found"))
            : disputeRepository.findByIdAndTradeOrderAccountClientUserUsername(id, username)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Dispute not found"));
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
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw error(HttpStatus.CONFLICT, "Dispute is already resolved");
        }
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedAt(LocalDateTime.now());
        return toResponse(disputeRepository.save(dispute));
    }

    private DisputeResponse toResponse(Dispute dispute) {
        TradeOrder order = dispute.getTradeOrder();
        return new DisputeResponse(dispute.getId(), order.getId(), order.getAccount().getId(),
            order.getValue(), dispute.getMessage(), dispute.getStatus(),
            dispute.getCreatedAt(), dispute.getResolvedAt());
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
