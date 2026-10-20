package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.ManagerTradeLogResponseDTO;
import com.buzzleapyear.trading_api.dto.ManagerTradeDetailResponseDTO;
import com.buzzleapyear.trading_api.dto.ManagerTradeStatusResponseDTO;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ManagerTradeLogService {
    private final TradeOrderRepository tradeOrderRepository;
    private final TradeOrderStatusRepository statusRepository;

    public ManagerTradeLogService(
        TradeOrderRepository tradeOrderRepository,
        TradeOrderStatusRepository statusRepository
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.statusRepository = statusRepository;
    }

    public Page<ManagerTradeLogResponseDTO> getTradeLog(Pageable pageable) {
        return tradeOrderRepository.findAllForManager(pageable).map(this::toDto);
    }

    public Optional<ManagerTradeDetailResponseDTO> getTradeDetail(Long tradeId) {
        return tradeOrderRepository.findForManagerById(tradeId)
            .map(order -> new ManagerTradeDetailResponseDTO(
                toDto(order),
                statusRepository.findByTradeOrderIdOrderByTimeUpdatedAscIdAsc(order.getId()).stream()
                    .map(status -> new ManagerTradeStatusResponseDTO(
                        status.getStatus(),
                        status.getTimeUpdated(),
                        status.getReasonText()
                    ))
                    .toList()
            ));
    }

    private ManagerTradeLogResponseDTO toDto(TradeOrder order) {
        Account account = order.getAccount();
        Client client = account == null ? null : account.getClient();
        User user = client == null ? null : client.getUser();
        Instrument instrument = order.getInstrument();

        List<TradeOrderStatus> statuses = statusRepository.findByTradeOrderId(order.getId());
        TradeOrderStatus latestStatus = statuses.stream()
            .max(Comparator.comparing(TradeOrderStatus::getTimeUpdated))
            .orElse(null);

        String clientName = user == null
            ? ""
            : (user.getFirstName() + " " + user.getLastName()).trim();

        return new ManagerTradeLogResponseDTO(
            order.getId(),
            order.getOrderDate(),
            account == null ? null : account.getId(),
            account == null ? "" : account.getAccountName(),
            client == null ? null : client.getId(),
            clientName,
            user == null ? "" : user.getEmail(),
            instrument == null ? "" : instrument.getInstrumentName(),
            instrument == null ? "" : instrument.getInstrumentSymbol(),
            instrument == null || instrument.getAssetType() == null ? "" : instrument.getAssetType().name(),
            instrument == null ? "" : instrument.getCurrencyCode(),
            order.getSide() == null ? "" : order.getSide().name(),
            order.getQuantity(),
            order.getPrice(),
            order.getValue(),
            latestStatus == null ? null : latestStatus.getStatus(),
            latestStatus == null ? null : latestStatus.getTimeUpdated(),
            latestStatus == null ? null : latestStatus.getReasonText(),
            findStatusTime(statuses, TradeOrderStatus.OrderStatus.SUBMITTED),
            findStatusTime(statuses, TradeOrderStatus.OrderStatus.FILLED)
        );
    }

    private LocalDateTime findStatusTime(
        List<TradeOrderStatus> statuses,
        TradeOrderStatus.OrderStatus targetStatus
    ) {
        return statuses.stream()
            .filter(status -> status.getStatus() == targetStatus)
            .map(TradeOrderStatus::getTimeUpdated)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }
}
