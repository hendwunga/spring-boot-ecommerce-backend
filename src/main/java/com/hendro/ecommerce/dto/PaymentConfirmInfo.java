package com.hendro.ecommerce.dto;

import lombok.Data;

@Data
public class PaymentConfirmInfo {

    private String paymentMethodId;

    private String token;

}
