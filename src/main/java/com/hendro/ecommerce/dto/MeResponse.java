package com.hendro.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeResponse {

    private String email;

    private String firstName;

    private String lastName;

    private String provider;

}
