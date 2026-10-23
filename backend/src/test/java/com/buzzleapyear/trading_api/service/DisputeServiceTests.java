package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.dto.DisputeResponse;
import com.buzzleapyear.trading_api.entity.*;
import com.buzzleapyear.trading_api.repository.DisputeRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

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

    @ParameterizedTest
    @CsvSource({
        "1, 9999.99, NORMAL",
        "1, 10000.00, CRITICAL",
        "1, 10000.01, CRITICAL",
        "999.9994, 10.00, NORMAL",
        "999.9995, 10.00, CRITICAL",
        "100.0000, 100.00, CRITICAL"
    })
    void createClassifiesRoundedExecutionValue(String quantity, String price, DisputeSeverity severity) {
        TradeOrder order = orderOwnedBy("owner");
        order.setQuantity(new BigDecimal(quantity));
        order.setPrice(new BigDecimal(price));
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = disputeService.create(new CreateDisputeRequest(7L, null), "owner");

        assertEquals(severity, response.severity());
        assertEquals(DisputeStatus.OPEN, response.status());
    }

    @ParameterizedTest
    @EnumSource(value = TradeOrderStatus.OrderStatus.class, names = "FILLED", mode = EnumSource.Mode.EXCLUDE)
    void createRejectsOrdersWithoutSuccessfulExecution(TradeOrderStatus.OrderStatus status) {
        TradeOrder order = orderOwnedBy("owner");
        order.getStatuses().get(0).setStatus(status);
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));

        var exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.create(new CreateDisputeRequest(7L, null), "owner"));

        assertEquals(409, exception.getStatusCode().value());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void createRejectsOrdersWithoutStatusHistory() {
        TradeOrder order = orderOwnedBy("owner");
        order.getStatuses().clear();
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class,
            () -> disputeService.create(new CreateDisputeRequest(7L, null), "owner"));
        verify(disputeRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(TradeOrder.OrderSide.class)
    void allReadAndResolvePathsReflectCurrentExecutionValue(TradeOrder.OrderSide side) {
        Dispute dispute = disputeFor("owner", DisputeStatus.OPEN);
        TradeOrder order = dispute.getTradeOrder();
        order.setSide(side);
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));
        assertEquals(DisputeSeverity.NORMAL, disputeService.get(4L, "owner", false).severity());

        order.setQuantity(new BigDecimal("100"));
        order.setPrice(new BigDecimal("100.00"));
        assertEquals(DisputeSeverity.CRITICAL, disputeService.get(4L, "owner", false).severity());

        PageRequest pageable = PageRequest.of(0, 20);
        when(disputeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Dispute>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(dispute)));
        assertEquals(DisputeSeverity.CRITICAL,
            disputeService.search("manager", true, null, null, null, null, null, null, null, pageable)
                .getContent().get(0).severity());

        when(disputeRepository.save(dispute)).thenReturn(dispute);
        var resolved = disputeService.resolve(4L, "Reviewed", authentication("manager"));
        assertEquals(DisputeStatus.RESOLVED, resolved.status());
        assertEquals(DisputeSeverity.CRITICAL, resolved.severity());
        assertEquals(DisputeSeverity.CRITICAL,
            disputeService.resolve(4L, "Reviewed again", authentication("manager")).severity());

        order.setPrice(BigDecimal.ONE);
        assertEquals(DisputeSeverity.NORMAL, disputeService.get(4L, "owner", false).severity());
    }

    @ParameterizedTest
    @CsvSource({", 25.00", "1, ", "0, 25.00", "-1, 25.00", "1, 0", "1, -25.00"})
    void invalidExecutionDataIsAnExplicitError(String quantity, String price) {
        Dispute dispute = disputeFor("owner", DisputeStatus.OPEN);
        dispute.getTradeOrder().setQuantity(quantity == null ? null : new BigDecimal(quantity));
        dispute.getTradeOrder().setPrice(price == null ? null : new BigDecimal(price));
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));

        var exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.get(4L, "owner", false));

        assertEquals(500, exception.getStatusCode().value());
    }

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
    void managerWhoOwnsAccountCanCreateDispute() {
        TradeOrder order = orderOwnedBy("manager");
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        DisputeResponse response = disputeService.create(
            new CreateDisputeRequest(7L, "  details  "), "manager");

        assertEquals(8L, response.id());
        assertEquals("details", response.message());
        verify(disputeRepository).save(any(Dispute.class));
    }

    @Test
    void whitespaceOnlyMessageIsStoredAsNull() {
        TradeOrder order = orderOwnedBy("owner");
        when(tradeOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        disputeService.create(new CreateDisputeRequest(7L, " \t "), "owner");

        verify(disputeRepository).save(argThat(dispute -> dispute.getMessage() == null));
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

        dispute.setMessage("Original dispute");
        var response = disputeService.resolve(4L, "  Issue corrected  ", manager);

        assertEquals(DisputeStatus.RESOLVED, response.status());
        assertNotNull(response.resolvedAt());
        assertEquals("Issue corrected", response.resolutionMessage());
        assertEquals("Original dispute", response.message());
        assertEquals("Issue corrected", dispute.getResolutionMessage());
        verify(disputeRepository).save(dispute);
    }

    @ParameterizedTest
    @EnumSource(DisputeStatus.class)
    void searchAppliesStatusPredicateBeforeRepositoryPagination(DisputeStatus status) {
        var pageable = PageRequest.of(0, 20);
        when(disputeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Dispute>>any(), eq(pageable)))
            .thenAnswer(invocation -> {
                Specification<Dispute> spec = invocation.getArgument(0);
                jakarta.persistence.criteria.Root<Dispute> root = mock(jakarta.persistence.criteria.Root.class);
                jakarta.persistence.criteria.CriteriaBuilder builder = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
                jakarta.persistence.criteria.Path<Object> path = mock(jakarta.persistence.criteria.Path.class);
                when(root.get("status")).thenReturn(path);
                spec.toPredicate(root, null, builder);
                verify(builder).equal(path, status);
                return new PageImpl<Dispute>(List.of(), pageable, 0);
            });
        assertTrue(disputeService.search("manager", true, null, null, null, null, null, status, null, pageable).isEmpty());
    }

    @Test
    void userFilterIncludesAllOwnedAccountsWithoutRemovingNonManagerOwnershipScope() {
        var pageable = PageRequest.of(0, 20);
        when(disputeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Dispute>>any(), eq(pageable)))
            .thenAnswer(invocation -> {
                Specification<Dispute> spec = invocation.getArgument(0);
                jakarta.persistence.criteria.Root<Dispute> root =
                    mock(jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
                jakarta.persistence.criteria.CriteriaBuilder builder =
                    mock(jakarta.persistence.criteria.CriteriaBuilder.class);
                var account = root.get("tradeOrder").get("account");
                var user = account.get("client").get("user");
                spec.toPredicate(root, null, builder);
                verify(builder).equal(user.get("id"), 42L);
                verify(builder).equal(user.get("username"), "owner");
                verify(account, never()).get("id");
                verify(root, never()).get("status");
                return new PageImpl<Dispute>(List.of(), pageable, 0);
            });

        assertTrue(disputeService.search("owner", false, null, null, null, null, null, null, 42L, pageable).isEmpty());
    }

    @Test
    void userAndAccountFiltersAreCombinedForManagerSearches() {
        var pageable = PageRequest.of(0, 20);
        when(disputeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Dispute>>any(), eq(pageable)))
            .thenAnswer(invocation -> {
                Specification<Dispute> spec = invocation.getArgument(0);
                jakarta.persistence.criteria.Root<Dispute> root =
                    mock(jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
                jakarta.persistence.criteria.CriteriaBuilder builder =
                    mock(jakarta.persistence.criteria.CriteriaBuilder.class);
                var account = root.get("tradeOrder").get("account");
                var user = account.get("client").get("user");
                spec.toPredicate(root, null, builder);
                verify(builder).equal(user.get("id"), 42L);
                verify(builder).equal(account.get("id"), 11L);
                verify(user, never()).get("username");
                return new PageImpl<Dispute>(List.of(), pageable, 0);
            });

        assertTrue(disputeService.search("manager", true, 11L, null, null, null, null, null, 42L, pageable).isEmpty());
    }

    @Test
    void managerCannotResolveDisputeForOwnedAccount() {
        Dispute dispute = disputeFor("manager", DisputeStatus.OPEN);
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));
        var manager = authentication("manager");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, "Reviewed", manager));

        assertEquals(403, exception.getStatusCode().value());
        assertEquals(DisputeStatus.OPEN, dispute.getStatus());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void authorizedManagerResolutionIsIdempotent() {
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 10, 22, 16, 0);
        Dispute dispute = disputeFor("owner", DisputeStatus.RESOLVED);
        dispute.setResolvedAt(resolvedAt);
        dispute.setResolutionMessage("Original resolution");
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));

        DisputeResponse response = disputeService.resolve(4L, "Replacement", authentication("manager"));

        assertEquals(DisputeStatus.RESOLVED, response.status());
        assertEquals(resolvedAt, response.resolvedAt());
        assertEquals("Original resolution", response.resolutionMessage());
        verify(disputeRepository, never()).save(any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {" ", "\t\n"})
    void resolveRejectsMissingOrBlankText(String text) {
        var exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, text, authentication("manager")));
        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(disputeRepository);
    }

    @Test
    void resolveEnforcesExactLengthBoundary() {
        var exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, "x".repeat(2001), authentication("manager")));
        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(disputeRepository);
        Dispute dispute = disputeFor("owner", DisputeStatus.OPEN);
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(dispute)).thenReturn(dispute);
        assertEquals("x".repeat(2000),
            disputeService.resolve(4L, "x".repeat(2000), authentication("manager")).resolutionMessage());
    }

    @Test
    void resolveRejectsNonManagersAndUnauthenticatedCallers() {
        var client = new UsernamePasswordAuthenticationToken("client", "password",
            List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        assertEquals(403, assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, "Reviewed", client)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, "Reviewed", null)).getStatusCode().value());
        verifyNoInteractions(disputeRepository);
    }

    @Test
    void resolveRejectsUnknownDisputes() {
        when(disputeRepository.findById(4L)).thenReturn(Optional.empty());
        var exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.resolve(4L, "Reviewed", authentication("manager")));
        assertEquals(404, exception.getStatusCode().value());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void resolvingLegacyResolvedRecordDoesNotAddTextOrChangeTimestamp() {
        Dispute dispute = disputeFor("owner", DisputeStatus.RESOLVED);
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 10, 22, 16, 0);
        dispute.setResolvedAt(resolvedAt);
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));
        var response = disputeService.resolve(4L, "New text", authentication("manager"));
        assertNull(response.resolutionMessage());
        assertEquals(resolvedAt, response.resolvedAt());
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void clientCannotViewAnotherOwnersDispute() {
        Dispute dispute = disputeFor("owner", DisputeStatus.OPEN);
        when(disputeRepository.findById(4L)).thenReturn(Optional.of(dispute));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.get(4L, "other", false));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void missingDisputeReturnsNotFound() {
        when(disputeRepository.findById(4L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> disputeService.get(4L, "owner", false));

        assertEquals(404, exception.getStatusCode().value());
    }

    private Dispute disputeFor(String owner, DisputeStatus status) {
        Dispute dispute = new Dispute();
        dispute.setId(4L);
        dispute.setTradeOrder(orderOwnedBy(owner));
        dispute.setStatus(status);
        return dispute;
    }

    private UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, "password",
            java.util.List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
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
        order.setQuantity(BigDecimal.ONE);
        order.setPrice(new BigDecimal("25.00"));
        TradeOrderStatus filled = new TradeOrderStatus();
        filled.setTradeOrder(order);
        filled.setStatus(TradeOrderStatus.OrderStatus.FILLED);
        order.addStatus(filled);
        return order;
    }
}
