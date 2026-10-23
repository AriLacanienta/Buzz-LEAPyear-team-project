package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.CreateDisputeRequest;
import com.buzzleapyear.trading_api.dto.DisputeResponse;
import com.buzzleapyear.trading_api.entity.DisputeStatus;
import com.buzzleapyear.trading_api.entity.DisputeSeverity;
import com.buzzleapyear.trading_api.service.DisputeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DisputeControllerTests {
    @Mock
    private DisputeService disputeService;
    @InjectMocks
    private DisputeController disputeController;

    private UsernamePasswordAuthenticationToken client;
    private UsernamePasswordAuthenticationToken manager;

    @BeforeEach
    void setUp() {
        client = authentication("client", "ROLE_CLIENT");
        manager = authentication("manager", "ROLE_MANAGER");
    }

    @Test
    void createReturnsCreatedResponseAndAuthenticatedUsername() {
        CreateDisputeRequest request = new CreateDisputeRequest(7L, "details");
        DisputeResponse response = response(4L, DisputeStatus.OPEN);
        when(disputeService.create(request, "client")).thenReturn(response);

        var result = disputeController.create(request, client);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
        assertEquals(DisputeSeverity.CRITICAL, result.getBody().severity());
        verify(disputeService).create(request, "client");
    }

    @Test
    void createRejectsNullTradeOrderIdWhenCalledDirectly() {
        when(disputeService.create(any(), anyString()))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trade order ID is required"));

        var result = assertThrows(ResponseStatusException.class,
            () -> disputeController.create(new CreateDisputeRequest(null, null), client));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void getSerializesSeveritySeparatelyFromLifecycleStatus() throws Exception {
        when(disputeService.get(4L, "manager", true))
            .thenReturn(response(4L, DisputeStatus.RESOLVED));

        MockMvcBuilders.standaloneSetup(disputeController).build()
            .perform(get("/api/v1/dispute/4").principal(manager))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.severity").value("CRITICAL"))
            .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void searchRejectsUnknownStatus() throws Exception {
        MockMvcBuilders.standaloneSetup(disputeController).build()
            .perform(get("/api/v1/dispute/search").param("status", "INVALID").principal(manager))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(disputeService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(DisputeStatus.class)
    void searchForwardsStatusFilter(DisputeStatus filter) throws Exception {
        when(disputeService.search(eq("manager"), eq(true), isNull(), isNull(), isNull(),
            isNull(), isNull(), eq(filter), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));
        MockMvcBuilders.standaloneSetup(disputeController).build()
            .perform(get("/api/v1/dispute/search").param("status", filter.name()).principal(manager))
            .andExpect(status().isOk());
        verify(disputeService).search(eq("manager"), eq(true), isNull(), isNull(), isNull(),
            isNull(), isNull(), eq(filter), any(Pageable.class));
    }

    @Test
    void searchRejectsInvalidSortField() {
        var result = disputeController.search(
            0, 20, "invalid", "desc", null, null, null, null, null, null, client);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(disputeService);
    }

    @Test
    void searchRejectsInvalidRanges() {
        var result = disputeController.search(
            0, 20, "createdAt", "desc", null,
            LocalDateTime.of(2026, 10, 23, 0, 0),
            LocalDateTime.of(2026, 10, 22, 0, 0),
            BigDecimal.TEN, BigDecimal.ONE, null, client);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(disputeService);
    }

    @Test
    void clientSearchUsesOwnerScopeAndDefaults() {
        when(disputeService.search(eq("client"), eq(false), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(response(4L, DisputeStatus.OPEN))));

        var result = disputeController.search(
            0, 20, "createdAt", "desc", null, null, null, null, null, null, client);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(disputeService).search(eq("client"), eq(false), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), argThat(pageable ->
                pageable.getPageNumber() == 0
                    && pageable.getPageSize() == 20
                    && pageable.getSort().getOrderFor("createdAt").isDescending()));
    }

    @Test
    void managerSearchUsesGlobalScope() {
        when(disputeService.search(eq("manager"), eq(true), eq(11L), any(), any(),
            eq(BigDecimal.ONE), eq(BigDecimal.TEN), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        disputeController.search(1, 10, "tradeValue", "asc", 11L,
            LocalDateTime.MIN, LocalDateTime.MAX, BigDecimal.ONE, BigDecimal.TEN, null, manager);

        verify(disputeService).search(eq("manager"), eq(true), eq(11L), any(), any(),
            eq(BigDecimal.ONE), eq(BigDecimal.TEN), isNull(), argThat(pageable ->
                pageable.getSort().getOrderFor("tradeOrder.tradeValue") != null));
    }

    @Test
    void resolveAcceptsRequiredTextAndSerializesSavedResolution() throws Exception {
        DisputeResponse response = new DisputeResponse(4L, 7L, "NAME", 11L,
            BigDecimal.TEN, "Original", DisputeStatus.RESOLVED, DisputeSeverity.NORMAL,
            LocalDateTime.now(), LocalDateTime.now(), "Reviewed");
        when(disputeService.resolve(4L, "Reviewed", manager)).thenReturn(response);
        MockMvcBuilders.standaloneSetup(disputeController).build()
            .perform(patch("/api/v1/dispute/4/resolve").principal(manager)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"resolutionMessage\":\"Reviewed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolutionMessage").value("Reviewed"))
            .andExpect(jsonPath("$.message").value("Original"))
            .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "", "{}", "{\"resolutionMessage\":null}", "{\"resolutionMessage\":\"\"}",
        "{\"resolutionMessage\":\"   \"}"
    })
    void resolveRejectsMissingAndBlankBody(String body) throws Exception {
        MockMvcBuilders.standaloneSetup(disputeController).build()
            .perform(patch("/api/v1/dispute/4/resolve").principal(manager)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(disputeService);
    }

    @Test
    void resolveValidatesLengthBoundary() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(disputeController).build();
        mvc.perform(patch("/api/v1/dispute/4/resolve").principal(manager)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"resolutionMessage\":\"" + "x".repeat(2001) + "\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(disputeService);
        when(disputeService.resolve(4L, "x".repeat(2000), manager))
            .thenReturn(response(4L, DisputeStatus.RESOLVED));
        mvc.perform(patch("/api/v1/dispute/4/resolve").principal(manager)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"resolutionMessage\":\"" + "x".repeat(2000) + "\"}"))
            .andExpect(status().isOk());
    }

    private UsernamePasswordAuthenticationToken authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "password",
            List.of(new SimpleGrantedAuthority(role)));
    }

    private DisputeResponse response(Long id, DisputeStatus status) {
        return new DisputeResponse(id, 7L, "NAME", 11L, new BigDecimal("10000.00"), "details", status,
            DisputeSeverity.CRITICAL,
            LocalDateTime.now(), null, null);
    }
}
