package com.hendro.ecommerce.config;

import com.okta.spring.boot.oauth.Okta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Use the new authorizeHttpRequests DSL
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        .anyRequest().permitAll()
                )
                // Enable JWT resource server support
                // Accepts both local HS256 JWTs (/api/auth/login) and Okta JWTs
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                )
                // Enable CORS
                .cors(cors -> {}) // You can further customize CORS if needed
                // CSRF protection as needed (optional)
                .csrf(csrf -> csrf.disable());

        // Add content negotiation strategy
        http.setSharedObject(ContentNegotiationStrategy.class,
                new HeaderContentNegotiationStrategy());

        // Okta 401 response body
        Okta.configureResourceServer401ResponseBody(http);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${okta.oauth2.issuer}") String oktaIssuer,
                                 @Value("${app.jwt.secret}") String jwtSecret) {

        // Decoder for locally-issued JWTs (HS256) from /api/auth/login
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder localDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();

        // Decoder for Okta-issued JWTs (RS256). Built lazily on first use so the
        // discovery document / JWKS is only fetched when an Okta token is actually decoded.
        AtomicReference<JwtDecoder> oktaDecoderRef = new AtomicReference<>();

        return token -> {
            try {
                return localDecoder.decode(token);
            } catch (JwtException ex) {
                JwtDecoder oktaDecoder = oktaDecoderRef.get();
                if (oktaDecoder == null) {
                    oktaDecoder = NimbusJwtDecoder.withIssuerLocation(oktaIssuer).build();
                    oktaDecoderRef.set(oktaDecoder);
                }
                return oktaDecoder.decode(token);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
