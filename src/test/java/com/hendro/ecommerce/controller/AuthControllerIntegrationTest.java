package com.hendro.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hendro.ecommerce.TestOAuth2ClientConfig;
import com.hendro.ecommerce.dao.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestOAuth2ClientConfig.class)
class AuthControllerIntegrationTest {

    private static final String REGISTER_BODY = """
            {"firstName":"Rina","lastName":"Melati","email":"rina@example.com","password":"password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    private String token;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        token = null;
    }

    @Test
    void register_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("rina@example.com"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void login_returns200WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BODY));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rina@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        token = objectMapper.readTree(response).get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rina@example.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withToken_returns200() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String theToken = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + theToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rina@example.com"))
                .andExpect(jsonPath("$.provider").value("local"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void orders_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void orders_withToken_returns200() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sari","lastName":"Dewi","email":"sari@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String theToken = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + theToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orders").isArray());
    }

}
