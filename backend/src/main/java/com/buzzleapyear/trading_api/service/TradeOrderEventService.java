package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.TradeEventOutbox;
import com.buzzleapyear.trading_api.repository.TradeEventOutboxRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.messaging.TradeOrderEvent;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

@Service
public class TradeOrderEventService {
    private final TradeOrderRepository orderRepository;
    private final TradeEventOutboxRepository outboxRepository;
    private final TradeOrderStatusRepository statusRepository;
    private final ObjectMapper objectMapper;

    public TradeOrderEventService(
            TradeOrderRepository orderRepository,
            TradeEventOutboxRepository outboxRepository,
            TradeOrderStatusRepository statusRepository,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.statusRepository = statusRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TradeOrder createOrder(TradeOrder order) {
        TradeOrder saved = orderRepository.save(order);
        enqueue(saved, "ORDER_CREATED", null);

        recordStatus(saved, OrderStatus.SUBMITTED, null);
        return saved;
    }

    @Transactional
    public void recordStatus(
            TradeOrder order,
            OrderStatus status,
            String reason) {
        TradeOrderStatus statusRecord = new TradeOrderStatus();
        statusRecord.setTradeOrder(order);
        statusRecord.setStatus(status);
        statusRecord.setTimeUpdated(LocalDateTime.now());
        statusRecord.setReasonText(reason);

        statusRepository.save(statusRecord);
        enqueue(order, "ORDER_STATUS_CHANGED", statusRecord);
    }

    private void enqueue(
            TradeOrder order,
            String eventType,
            TradeOrderStatus statusRecord) {
        UUID eventId = UUID.randomUUID();
        var account = order.getAccount();
        var instrument = order.getInstrument();

        TradeOrderEvent event = new TradeOrderEvent(
            eventId,
            eventType,
            order.getId(),
            account.getId(),
            account.getClient().getId(),
            instrument.getId(),
            account.getClient().getUser().getFirstName() + " " + account.getClient().getUser().getLastName(),
            account.getAccountName(),
            instrument.getInstrumentSymbol(),
            instrument.getAssetType().name(),
            instrument.getCurrencyCode(),
            order.getSide().name(),
            order.getQuantity(),
            order.getPrice(),
            order.getValue(),
            order.getOrderDate(),
            statusRecord == null ? null : statusRecord.getStatus().name(),
            statusRecord == null ? null : statusRecord.getTimeUpdated(),
            statusRecord == null ? null : statusRecord.getReasonText()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new TradeEventOutbox(
                eventId, account.getId(), eventType, payload, LocalDateTime.now()));
            
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize trade order event", e);
        }
    }
}
