package com.hendro.ecommerce.controller;

import com.hendro.ecommerce.dto.AuthResponse;
import com.hendro.ecommerce.dto.LoginRequest;
import com.hendro.ecommerce.dto.MeResponse;
import com.hendro.ecommerce.dto.RegisterRequest;
import com.hendro.ecommerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrasi akun baru", description = "Membuat akun user baru dan langsung mengembalikan JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Akun berhasil dibuat",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email sudah terdaftar / data tidak valid")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login untuk mendapatkan JWT", description = "Memvalidasi kredensial dan mengembalikan JWT untuk akses endpoint terproteksi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login sukses",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email atau password salah")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/me")
    @Operation(summary = "Profil user dari token JWT", description = "Mengembalikan data user berdasarkan token Bearer. Mendukung token lokal (/api/auth/login) maupun token Okta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil ditemukan",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token tidak valid / tidak ada")
    })
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getSubject();
        }

        return new ResponseEntity<>(authService.findProfile(email), HttpStatus.OK);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

}
