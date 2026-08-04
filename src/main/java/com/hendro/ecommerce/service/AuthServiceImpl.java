package com.hendro.ecommerce.service;

import com.hendro.ecommerce.dao.AppUserRepository;
import com.hendro.ecommerce.dto.AuthResponse;
import com.hendro.ecommerce.dto.LoginRequest;
import com.hendro.ecommerce.dto.MeResponse;
import com.hendro.ecommerce.dto.RegisterRequest;
import com.hendro.ecommerce.entity.AppUser;
import com.hendro.ecommerce.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final long TOKEN_EXPIRY_MILLIS = 60 * 60 * 1000L;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return buildResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail().toLowerCase());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(AppUser user) {
        String token = jwtService.generateToken(user, TOKEN_EXPIRY_MILLIS);
        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName(), TOKEN_EXPIRY_MILLIS / 1000);
    }

    @Override
    public MeResponse findProfile(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        AppUser user = userRepository.findByEmail(email.toLowerCase());
        if (user != null) {
            return new MeResponse(user.getEmail(), user.getFirstName(), user.getLastName(), "local");
        }

        String localPart = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        return new MeResponse(email, localPart, "", "okta");
    }

}
