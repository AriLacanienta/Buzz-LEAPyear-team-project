package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.ManagerTradeLogResponseDTO;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.springframework.stereotype.Service;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
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
        return tradeOrderRepository.findAllForManager(pageable)
            .map(this::toDto);
    }

    private ManagerTradeLogResponseDTO toDto(TradeOrder order) {
        Account account = order.getAccount();
        Client client = account.getClient();
        User user = client.getUser();
        Instrument instrument = order.getInstrument();

        TradeOrderStatus status =
            statusRepository.findFirstByTradeOrderIdOrderByTimeUpdatedDesc(
                order.getId()
            );

        return new ManagerTradeLogResponseDTO(
            order.getId(),
            order.getOrderDate(),
            account.getId(),
            account.getAccountName(),
            client.getId(),
            user.getFirstName() + " " + user.getLastName(),
            user.getEmail(),
            instrument.getInstrumentName(),
            instrument.getInstrumentSymbol(),
            instrument.getAssetType().name(),
            instrument.getCurrencyCode(),
            order.getSide().name(),
            order.getQuantity(),
            order.getPrice(),
            order.getValue(),
            status == null ? null : status.getStatus(),
            status == null ? null : status.getTimeUpdated(),
            status == null ? null : status.getReasonText()
        );
    }
}