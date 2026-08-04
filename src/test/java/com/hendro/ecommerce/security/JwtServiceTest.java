package com.hendro.ecommerce.security;

import com.hendro.ecommerce.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!";

    private JwtService jwtService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        user = new AppUser();
        user.setFirstName("Andi");
        user.setLastName("Wijaya");
        user.setEmail("andi@example.com");
    }

    @Test
    void generateAndParse_roundTrip() {
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("andi@example.com", jwtService.getEmailFromToken(token));
    }

    @Test
    void tamperedSignature_isRejected() {
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(IllegalArgumentException.class, () -> jwtService.getEmailFromToken(tampered));
    }

    @Test
    void expiredToken_isRejected() {
        String token = jwtService.generateToken(user, -1000);

        assertThrows(IllegalArgumentException.class, () -> jwtService.getEmailFromToken(token));
    }

    @Test
    void malformedToken_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.getEmailFromToken("not-a-jwt"));
        assertThrows(IllegalArgumentException.class, () -> jwtService.getEmailFromToken("a.b"));
    }

}
