package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.RecentOrderResponseDTO;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradeOrderService {
    private final TradeOrderRepository tradeOrderRepository;

    public TradeOrderService(TradeOrderRepository tradeOrderRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
    }

    public List<RecentOrderResponseDTO> getRecentOrdersByAccountId(Long accountId, int limit) {
        List<TradeOrder> orders = tradeOrderRepository.findByAccountIdOrderByOrderDateDesc(accountId);
        
        return orders.stream()
            .limit(limit)
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private RecentOrderResponseDTO convertToDto(TradeOrder order) {
        String clientName = "";
        Long clientId = null;
        
        if (order.getAccount() != null && order.getAccount().getClient() != null) {
            var client = order.getAccount().getClient();
            clientId = client.getId();
            
            if (client.getUser() != null) {
                clientName = client.getUser().getFirstName() + " " + client.getUser().getLastName();
            }
        }
        
        String instrument = "";
        String assetClass = "";
        String currency = "";
        
        if (order.getInstrument() != null) {
            instrument = order.getInstrument().getInstrumentSymbol();
            assetClass = order.getInstrument().getAssetType().toString();
            currency = order.getInstrument().getCurrencyCode();
        }
        
        return new RecentOrderResponseDTO(
            order.getId(),
            order.getOrderDate(),
            clientId,
            clientName,
            instrument,
            assetClass,
            order.getSide().toString(),
            order.getQuantity(),
            order.getPrice(),
            currency,
            order.getValue()
        );
    }
}
