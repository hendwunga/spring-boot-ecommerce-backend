package com.hendro.ecommerce.controller;

import com.hendro.ecommerce.dto.PaymentConfirmInfo;
import com.hendro.ecommerce.dto.PaymentInfo;
import com.hendro.ecommerce.dto.Purchase;
import com.hendro.ecommerce.dto.PurchaseResponse;
import com.hendro.ecommerce.service.CheckoutService;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private Logger logger = Logger.getLogger(getClass().getName());
    private CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/purchase")
    public PurchaseResponse placeOrder(@RequestBody Purchase purchase) {

        PurchaseResponse purchaseResponse=checkoutService.placeOrder(purchase);
        return purchaseResponse;
    }
    @PostMapping("/payment-intent")
    public ResponseEntity<String> createPaymentIntent(@RequestBody PaymentInfo paymentInfo) throws StripeException {

        logger.info("paymentInfo.amount: " + paymentInfo.getAmount());
        PaymentIntent paymentIntent = checkoutService.createPaymentIntent(paymentInfo);

        String paymentStr = paymentIntent.toJson();

        return new ResponseEntity<>(paymentStr, HttpStatus.OK);
    }

    @GetMapping("/payment-intent/{id}")
    public ResponseEntity<String> getPaymentIntent(@PathVariable String id) throws StripeException {

        PaymentIntent paymentIntent = checkoutService.getPaymentIntent(id);
        return new ResponseEntity<>(paymentIntent.toJson(), HttpStatus.OK);
    }

    @PostMapping("/payment-intent/{id}/confirm")
    public ResponseEntity<?> confirmPaymentIntent(@PathVariable String id,
                                                  @RequestBody PaymentConfirmInfo paymentConfirmInfo) {

        try {
            PaymentIntent paymentIntent = checkoutService.confirmPaymentIntent(id, paymentConfirmInfo);
            return new ResponseEntity<>(paymentIntent.toJson(), HttpStatus.OK);
        } catch (CardException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Payment declined");
            error.put("declineCode", ex.getDeclineCode());
            error.put("message", ex.getMessage());
            error.put("paymentIntentId", id);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", ex.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (StripeException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Stripe request failed");
            error.put("message", ex.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
