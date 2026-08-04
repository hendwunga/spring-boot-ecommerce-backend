package com.hendro.ecommerce.service;

import com.hendro.ecommerce.dto.AuthResponse;
import com.hendro.ecommerce.dto.LoginRequest;
import com.hendro.ecommerce.dto.MeResponse;
import com.hendro.ecommerce.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MeResponse findProfile(String email);

}
