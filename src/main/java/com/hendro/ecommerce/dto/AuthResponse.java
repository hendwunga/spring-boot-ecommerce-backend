package com.hendro.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String email;

    private String firstName;

    private String lastName;

    private long expiresIn;

}
