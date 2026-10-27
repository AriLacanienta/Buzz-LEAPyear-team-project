package com.buzzleapyear.trading_api.security;

import com.buzzleapyear.trading_api.config.SecurityConfig;
import com.buzzleapyear.trading_api.controller.DisputeController;
import com.buzzleapyear.trading_api.dto.ManagerDashboardSummary;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.service.DisputeService;
import com.buzzleapyear.trading_api.service.ManagerDashboardService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DisputeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardSummarySecurityTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private DisputeService disputeService;
    @MockitoBean private ManagerDashboardService summaryService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void anonymousCannotReadSummary() throws Exception {
        mvc.perform(get("/api/v1/dispute/summary")).andExpect(status().isUnauthorized());
        verifyNoInteractions(summaryService, disputeService);
    }

    @Test
    void errorDispatchPreservesServerErrorInsteadOfReturningUnauthorized() throws Exception {
        mvc.perform(get("/error")
                .with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                })
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/dispute/summary"))
            .andExpect(status().isInternalServerError());
        verifyNoInteractions(summaryService, disputeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENT", "ANALYST"})
    void nonManagersCannotReadSummary(String role) throws Exception {
        mvc.perform(get("/api/v1/dispute/summary").with(user("other").roles(role)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(summaryService, disputeService);
    }

    @Test
    void managerReceivesCompleteSummaryScopedToAuthenticatedIdentity() throws Exception {
        when(summaryService.summary("manager")).thenReturn(new ManagerDashboardSummary(
            LocalDate.of(2026, 10, 22), "UTC", Instant.parse("2026-10-22T12:00:00Z"),
            3, 0, 3, null, 2, new BigDecimal("66.7"), 5, 2, 2,
            List.of(new ManagerDashboardSummary.CurrencyTotal("USD", new BigDecimal("100.00")))));

        mvc.perform(get("/api/v1/dispute/summary").with(user("manager").roles("MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.businessDate").value("2026-10-22"))
            .andExpect(jsonPath("$.timeZone").value("UTC"))
            .andExpect(jsonPath("$.asOf").value("2026-10-22T12:00:00Z"))
            .andExpect(jsonPath("$.ordersToday").value(3))
            .andExpect(jsonPath("$.ordersYesterday").value(0))
            .andExpect(jsonPath("$.ordersDifference").value(3))
            .andExpect(jsonPath("$.ordersChangePercent").isEmpty())
            .andExpect(jsonPath("$.filledOrdersToday").value(2))
            .andExpect(jsonPath("$.fillRatePercent").value(66.7))
            .andExpect(jsonPath("$.openDisputes").value(5))
            .andExpect(jsonPath("$.criticalOpenDisputes").value(2))
            .andExpect(jsonPath("$.activeClientsToday").value(2))
            .andExpect(jsonPath("$.assetsMovedByCurrency[0].currencyCode").value("USD"))
            .andExpect(jsonPath("$.assetsMovedByCurrency[0].totalValue").value(100.00));
        verify(summaryService).summary("manager");
        verifyNoInteractions(disputeService);
    }
}
