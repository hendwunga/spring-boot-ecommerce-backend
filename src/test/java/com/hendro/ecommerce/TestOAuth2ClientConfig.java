package com.hendro.ecommerce;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@TestConfiguration
public class TestOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("test")
                .clientId("test-client")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/test")
                .scope("openid")
                .authorizationUri("http://localhost:1/oauth2/authorize")
                .tokenUri("http://localhost:1/oauth2/token")
                .userInfoUri("http://localhost:1/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("http://localhost:1/keys")
                .clientName("test")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

}
