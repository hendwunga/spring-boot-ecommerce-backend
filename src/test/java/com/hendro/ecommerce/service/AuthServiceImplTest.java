package com.hendro.ecommerce.service;

import com.hendro.ecommerce.dao.AppUserRepository;
import com.hendro.ecommerce.dto.AuthResponse;
import com.hendro.ecommerce.dto.LoginRequest;
import com.hendro.ecommerce.dto.MeResponse;
import com.hendro.ecommerce.dto.RegisterRequest;
import com.hendro.ecommerce.entity.AppUser;
import com.hendro.ecommerce.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_createsUserAndReturnsToken() {
        when(userRepository.existsByEmail("budi@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(jwtService.generateToken(any(AppUser.class), anyLong())).thenReturn("jwt-token");

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Budi");
        request.setLastName("Santoso");
        request.setEmail("Budi@Example.com");
        request.setPassword("password123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("budi@example.com", response.getEmail());
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("budi@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Budi");
        request.setLastName("Santoso");
        request.setEmail("budi@example.com");
        request.setPassword("password123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void login_validCredentials_returnsToken() {
        AppUser user = new AppUser();
        user.setFirstName("Budi");
        user.setLastName("Santoso");
        user.setEmail("budi@example.com");
        user.setPassword("$2a$10$hashed");

        when(userRepository.findByEmail("budi@example.com")).thenReturn(user);
        when(passwordEncoder.matches("password123", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.generateToken(any(AppUser.class), anyLong())).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("Budi@Example.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("budi@example.com", response.getEmail());
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        AppUser user = new AppUser();
        user.setEmail("budi@example.com");
        user.setPassword("$2a$10$hashed");

        when(userRepository.findByEmail("budi@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("budi@example.com");
        request.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_unknownUser_throwsBadCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("anything");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void findProfile_localUser_returnsLocal() {
        AppUser user = new AppUser();
        user.setFirstName("Budi");
        user.setLastName("Santoso");
        user.setEmail("budi@example.com");

        when(userRepository.findByEmail("budi@example.com")).thenReturn(user);

        MeResponse response = authService.findProfile("budi@example.com");

        assertEquals("local", response.getProvider());
        assertEquals("Budi", response.getFirstName());
        assertEquals("Santoso", response.getLastName());
    }

    @Test
    void findProfile_unknownUser_returnsOktaFallback() {
        when(userRepository.findByEmail("okta@example.com")).thenReturn(null);

        MeResponse response = authService.findProfile("okta@example.com");

        assertEquals("okta", response.getProvider());
        assertEquals("okta@example.com", response.getEmail());
    }

}
