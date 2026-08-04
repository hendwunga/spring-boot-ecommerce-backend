package com.hendro.ecommerce.service;

import com.hendro.ecommerce.dto.PaymentConfirmInfo;
import com.hendro.ecommerce.dto.PaymentInfo;
import com.hendro.ecommerce.dto.Purchase;
import com.hendro.ecommerce.dto.PurchaseResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public interface CheckoutService {

     PurchaseResponse placeOrder(Purchase purchase);

     PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException;

     PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException;

     PaymentIntent confirmPaymentIntent(String paymentIntentId, PaymentConfirmInfo paymentConfirmInfo) throws StripeException;

     void updateOrderStatusByTrackingNumber(String trackingNumber);
}
