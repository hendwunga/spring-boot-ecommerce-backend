package com.hendro.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hendro.ecommerce.TestOAuth2ClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestOAuth2ClientConfig.class)
class CheckoutIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_123";

    private static final String PURCHASE_BODY = """
            {
              "customer": {"firstName":"Tono","lastName":"Wibowo","email":"tono@example.com"},
              "shippingAddress": {"street":"Jl. Dahlia 5","city":"Yogyakarta","state":"Yogyakarta","country":"Indonesia","zipCode":"55281"},
              "billingAddress": {"street":"Jl. Dahlia 5","city":"Yogyakarta","state":"Yogyakarta","country":"Indonesia","zipCode":"55281"},
              "order": {"totalQuantity":1,"totalPrice":44.99,"status":"PENDING"},
              "orderItems": [{"imageUrl":"https://picsum.photos/seed/book1002/400/400","unitPrice":44.99,"quantity":1,"productId":3}]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void placeOrder_createsOrderWithTrackingNumber() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/checkout/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURCHASE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingNumber").isNotEmpty())
                .andReturn();

        String trackingNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("orderTrackingNumber").asText();
        assertThat(trackingNumber).isNotBlank();
    }

    @Test
    void webhook_invalidSignature_returns400() throws Exception {
        mockMvc.perform(post("/api/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=deadbeef")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }

    @Test
    void webhook_validSignature_updatesOrderToPaid() throws Exception {
        String trackingNumber = createOrder();

        String payload = """
                {
                  "id": "evt_test_0001",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "pi_test_0001",
                      "object": "payment_intent",
                      "amount": 4499,
                      "currency": "usd",
                      "status": "succeeded",
                      "metadata": {"orderTrackingNumber": "%s"}
                    }
                  }
                }
                """.formatted(trackingNumber);

        String signature = sign(payload);

        mockMvc.perform(post("/api/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        String token = registerAndGetToken();
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orders[?(@.orderTrackingNumber=='" + trackingNumber + "')].status")
                        .value("PAID"));
    }

    private String registerAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Rudi","lastName":"Hartono","email":"rudi@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/checkout/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PURCHASE_BODY))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("orderTrackingNumber").asText();
    }

    private String sign(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String toSign = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }

        return "t=" + timestamp + ",v1=" + hex;
    }

}
