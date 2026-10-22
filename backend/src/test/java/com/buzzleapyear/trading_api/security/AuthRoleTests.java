package com.buzzleapyear.trading_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;

import com.buzzleapyear.trading_api.controller.AuthController;
import com.buzzleapyear.trading_api.dto.AuthResponse;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.RefreshSession;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.LoginAttemptRepository;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.RefreshSessionRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthRoleTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
            authController, "refreshSessionRepository", refreshSessionRepository);
    }

    @ParameterizedTest
    @EnumSource(Role.RoleName.class)
    void loginReturnsPersistedRoleNames(Role.RoleName roleName) {
        User user = userWithRoles(roleName);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(tokenProvider.generateAccessToken(user.getUsername(), 0)).thenReturn("access-token");

        ResponseEntity<?> response = authController.login(
            new LoginRequest(user.getEmail(), "password"), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertAuthResponse(response, List.of(roleName.name()));
    }

    @Test
    void loginDoesNotInventRolesForUsersWithoutAssignments() {
        User user = userWithRoles();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(tokenProvider.generateAccessToken(user.getUsername(), 0)).thenReturn("access-token");

        ResponseEntity<?> response = authController.login(
            new LoginRequest(user.getEmail(), "password"), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertAuthResponse(response, List.of());
    }

    @Test
    void refreshReturnsAllCurrentPersistedRoleNames() {
        User user = userWithRoles(Role.RoleName.CLIENT, Role.RoleName.MANAGER);
        RefreshSession session = new RefreshSession();
        session.setUsername(user.getUsername());
        session.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshSessionRepository.findByTokenHash(anyString())).thenReturn(Optional.of(session));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user.getUsername(), 0)).thenReturn("access-token");

        ResponseEntity<?> response = authController.refreshToken("refresh-token", httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertAuthResponse(response, List.of("CLIENT", "MANAGER"));
    }

    private User userWithRoles(Role.RoleName... roleNames) {
        User user = new User();
        user.setUsername("test-user");
        user.setEmail("test-user@example.com");
        user.setPasswordHash("hashed-password");
        for (Role.RoleName roleName : roleNames) {
            user.getUserRoles().add(new UserRole(user, new Role(roleName, roleName.name()), "test"));
        }
        return user;
    }

    private void assertAuthResponse(ResponseEntity<?> response, List<String> roles) {
        AuthResponse body = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals(roles, body.getRoles());
        assertEquals("test-user", body.getUsername());
        assertEquals("test-user@example.com", body.getEmail());
        assertEquals("access-token", body.getAccessToken());
        assertEquals(body.getAccessToken(), body.getToken());
        assertEquals("Bearer", body.getType());
        assertNull(body.getRefreshToken());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.startsWith("refresh_token="));
        assertTrue(cookie.contains("HttpOnly"));
    }

    @Test
    void loadUserByUsernameMapsPersistedRolesToSpringAuthorities() {
        User user = new User();
        user.setUsername("manager");
        user.setPasswordHash("hashed-password");

        Role managerRole = new Role(Role.RoleName.MANAGER, "Manager");
        user.getUserRoles().add(new UserRole(user, managerRole, "test"));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userRepository", userRepository);

        UserDetails details = service.loadUserByUsername("manager");

        assertEquals(List.of("ROLE_MANAGER"), details.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());
    }

    @Test
    void registrationAssignsClientRoleAndReturnsItInAuthResponse() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("new-client");
        request.setEmail("new-client@example.com");
        request.setFirstName("New");
        request.setLastName("Client");
        request.setPassword("password");

        Role clientRole = new Role(Role.RoleName.CLIENT, "Client");
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(Role.RoleName.CLIENT)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateAccessToken(request.getUsername(), 0)).thenReturn("token");
        when(httpServletRequest.isSecure()).thenReturn(false);
        when(httpServletRequest.getHeader("X-Forwarded-Proto")).thenReturn(null);
        ResponseEntity<?> response = authController.register(request, httpServletRequest);

        AuthResponse authResponse = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(List.of("CLIENT"), authResponse.getRoles());
        assertEquals("new-client", authResponse.getUsername());
        assertEquals("new-client@example.com", authResponse.getEmail());
        User savedUser = org.mockito.Mockito.mockingDetails(userRepository).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("save"))
            .map(invocation -> (User) invocation.getArgument(0))
            .findFirst()
            .orElseThrow();
        assertEquals(List.of("ROLE_CLIENT"), savedUser.getUserRoles().stream()
            .map(userRole -> "ROLE_" + userRole.getRole().getRoleName().name())
            .toList());
    }
}
