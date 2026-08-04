package com.hendro.ecommerce.service;

import com.hendro.ecommerce.dao.CustomerRepository;
import com.hendro.ecommerce.dao.OrderRepository;
import com.hendro.ecommerce.dto.PaymentConfirmInfo;
import com.hendro.ecommerce.dto.PaymentInfo;
import com.hendro.ecommerce.dto.Purchase;
import com.hendro.ecommerce.dto.PurchaseResponse;
import com.hendro.ecommerce.entity.Customer;
import com.hendro.ecommerce.entity.Order;
import com.hendro.ecommerce.entity.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public  class CheckoutServiceImpl implements CheckoutService{

    private CustomerRepository customerRepository;
    private OrderRepository orderRepository;

    public CheckoutServiceImpl(CustomerRepository customerRepository,
                               OrderRepository orderRepository,
                               @Value("${stripe.key.secret}") String secretKey) {

        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;

        // initialize Stripe API with secret key
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public PurchaseResponse placeOrder(Purchase purchase) {

        // retrieve the order info from dto
        Order order = purchase.getOrder();

        // generate tracking number
        String orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);

        // populate order with orderItems
        Set<OrderItem> orderItems = purchase.getOrderItems();
        orderItems.forEach(item -> order.add(item));

        // populate order with billingAddress and shippingAddress
        order.setBillingAddress(purchase.getBillingAddress());
        order.setShippingAddress(purchase.getShippingAddress());

        // populate customer with order
        Customer customer = purchase.getCustomer();

        // check if this is an existing customer
        String theEmail = customer.getEmail();

        Customer customerFromDb = customerRepository.findByEmail(theEmail);

        if(customerFromDb != null) {
            customer = customerFromDb;
        }
        customer.add(order);

        // save to the database
        customerRepository.save(customer);

        // return a response
        return new PurchaseResponse(orderTrackingNumber);
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException {

        List<String> paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card");

        Map<String, Object> params = new HashMap<>();
        params.put("amount", paymentInfo.getAmount());
        params.put("currency", paymentInfo.getCurrency());
        params.put("payment_method_types", paymentMethodTypes);
        params.put("description", "Hen Store - Purchase");
        params.put("receipt_email", paymentInfo.getReceiptEmail());

        if (paymentInfo.getOrderTrackingNumber() != null && !paymentInfo.getOrderTrackingNumber().isEmpty()) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderTrackingNumber", paymentInfo.getOrderTrackingNumber());
            params.put("metadata", metadata);
        }

        return PaymentIntent.create(params);
    }

    @Override
    public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    @Override
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, PaymentConfirmInfo paymentConfirmInfo) throws StripeException {

        Map<String, Object> confirmParams = new HashMap<>();

        String paymentMethod = paymentConfirmInfo.getPaymentMethodId();

        if (paymentMethod == null || paymentMethod.isEmpty()) {
            if (paymentConfirmInfo.getToken() != null && !paymentConfirmInfo.getToken().isEmpty()) {
                Map<String, Object> pmParams = new HashMap<>();
                pmParams.put("type", "card");
                Map<String, Object> card = new HashMap<>();
                card.put("token", paymentConfirmInfo.getToken());
                pmParams.put("card", card);
                paymentMethod = PaymentMethod.create(pmParams).getId();
            }
        }

        if (paymentMethod == null || paymentMethod.isEmpty()) {
            throw new IllegalArgumentException("paymentMethodId or token is required");
        }

        confirmParams.put("payment_method", paymentMethod);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntent confirmed = paymentIntent.confirm(confirmParams);

        if ("succeeded".equals(confirmed.getStatus())) {
            updateOrderStatus(confirmed);
        }

        return confirmed;
    }

    private void updateOrderStatus(PaymentIntent paymentIntent) {

        String trackingNumber = paymentIntent.getMetadata().get("orderTrackingNumber");

        if (trackingNumber == null || trackingNumber.isEmpty()) {
            return;
        }

        updateOrderStatusByTrackingNumber(trackingNumber);
    }

    @Override
    public void updateOrderStatusByTrackingNumber(String trackingNumber) {

        if (trackingNumber == null || trackingNumber.isEmpty()) {
            return;
        }

        Order order = orderRepository.findByOrderTrackingNumber(trackingNumber);

        if (order != null) {
            order.setStatus("PAID");
            orderRepository.save(order);
        }
    }

    private String generateOrderTrackingNumber() {

        // generate a random UUID number (UUID version-4)
        // For details see: https://en.wikipedia.org/wiki/Universally_unique_identifier
        //
        return UUID.randomUUID().toString();
    }
}