package com.buzzleapyear.trading_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;

import com.buzzleapyear.trading_api.controller.AuthController;
import com.buzzleapyear.trading_api.dto.AuthResponse;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
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
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

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
        org.springframework.test.util.ReflectionTestUtils.setField(
            authController, "refreshSessionRepository", refreshSessionRepository);
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
        assertEquals(201, response.getStatusCode().value());
        assertEquals("new-client", authResponse.getUsername());
        assertEquals("new-client@example.com", authResponse.getEmail());
        assertEquals(List.of("CLIENT"), authResponse.getRoles());
        assertEquals("token", authResponse.getAccessToken());
        assertEquals("token", authResponse.getToken());
        assertNull(authResponse.getRefreshToken());
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
