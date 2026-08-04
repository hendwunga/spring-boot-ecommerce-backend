package com.hendro.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hendro.ecommerce.service.CheckoutService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@Tag(name = "Webhook")
public class WebhookController {

    private final CheckoutService checkoutService;
    private final String endpointSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookController(CheckoutService checkoutService,
                             @Value("${stripe.webhook.secret}") String endpointSecret) {
        this.checkoutService = checkoutService;
        this.endpointSecret = endpointSecret;
    }

    @PostMapping("/stripe")
    @Operation(summary = "Terima event dari Stripe",
            description = "Verifikasi signature Stripe lalu update status order menjadi PAID saat event payment_intent.succeeded")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event diterima",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Signature tidak valid")
    })
    public ResponseEntity<Map<String, Object>> handleStripeEvent(@RequestBody String payload,
                                                                 @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException ex) {
            return new ResponseEntity<>(Map.of("error", "Invalid signature"), HttpStatus.BAD_REQUEST);
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            checkoutService.updateOrderStatusByTrackingNumber(extractTrackingNumber(payload));
        }

        return new ResponseEntity<>(Map.of("received", true), HttpStatus.OK);
    }

    private String extractTrackingNumber(String payload) {
        try {
            JsonNode metadata = objectMapper.readTree(payload)
                    .path("data").path("object").path("metadata");
            String trackingNumber = metadata.path("orderTrackingNumber").asText(null);
            return trackingNumber == null || trackingNumber.isEmpty() ? null : trackingNumber;
        } catch (Exception ex) {
            return null;
        }
    }

}
