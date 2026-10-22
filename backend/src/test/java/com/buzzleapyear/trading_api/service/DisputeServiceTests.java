package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.entity.*;
import com.buzzleapyear.trading_api.repository.DisputeRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTests {
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private TradeOrderRepository tradeOrderRepository;
    @InjectMocks
    private DisputeService disputeService;

    @Test
    void createRequiresTradeOrderAccountOwner() {
        TradeOrder order = orderOwnedBy("owner");
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.create(new CreateDisputeRequest(7L, "details"), "other"));

        assertEquals(403, exception.getStatusCode().value());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void createRejectsSecondOpenDispute() {
        TradeOrder order = orderOwnedBy("owner");
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(disputeRepository.existsByTradeOrderIdAndStatus(7L, DisputeStatus.OPEN)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.create(new CreateDisputeRequest(7L, null), "owner"));

        assertEquals(409, exception.getStatusCode().value());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void managerCanResolveOpenDispute() {
        Dispute dispute = new Dispute();
        dispute.setId(4L);
        dispute.setTradeOrder(orderOwnedBy("owner"));
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(dispute)).thenReturn(dispute);
        var manager = new UsernamePasswordAuthenticationToken("manager", "password",
            java.util.List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        var response = disputeService.resolve(4L, manager);

        assertEquals(DisputeStatus.RESOLVED, response.status());
        assertNotNull(response.resolvedAt());
        verify(disputeRepository).save(dispute);
    }

    private TradeOrder orderOwnedBy(String username) {
        User user = new User();
        user.setUsername(username);
        Client client = new Client();
        client.setUser(user);
        Account account = new Account();
        account.setId(11L);
        account.setClient(client);
        TradeOrder order = new TradeOrder();
        order.setId(7L);
        order.setAccount(account);
        order.setValue(new BigDecimal("25.00"));
        return order;
    }
}
