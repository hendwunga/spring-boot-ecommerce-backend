package com.hendro.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot E-Commerce API")
                        .version("1.0.0")
                        .description("""
                                Backend API untuk platform E-Commerce.

                                - **Register / Login**: `/api/auth/register` dan `/api/auth/login`
                                  mengembalikan JWT yang dipakai pada endpoint terproteksi
                                  (contoh: `/api/orders/**`) melalui tombol **Authorize**.
                                - **Katalog**: produk, kategori, negara & state (read-only).
                                - **Checkout**: buat order dan proses pembayaran via Stripe.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Masukkan JWT dari /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer cleanTagCustomizer() {
        return openApi -> {
            Map<String, String> tagDescriptions = new LinkedHashMap<>();
            tagDescriptions.put("Authentication", "Registrasi akun dan login untuk mendapatkan JWT");
            tagDescriptions.put("Checkout", "Buat order dan proses pembayaran via Stripe");
            tagDescriptions.put("Webhook", "Terima event dari Stripe untuk update status pembayaran");
            tagDescriptions.put("Order", "Kelola pesanan (terproteksi - butuh JWT)");
            tagDescriptions.put("Product", "Katalog produk");
            tagDescriptions.put("Product Category", "Kategori produk");
            tagDescriptions.put("Country", "Daftar negara");
            tagDescriptions.put("State", "Daftar state/provinsi per negara");
            tagDescriptions.put("Customer", "Data pelanggan");
            tagDescriptions.put("API Profile", "Metadata schema Spring Data REST");

            openApi.getPaths().forEach((path, pathItem) -> {
                String tag = resolveTag(path);
                pathItem.readOperations().forEach(op -> op.setTags(Collections.singletonList(tag)));
            });

            Set<String> usedTags = new LinkedHashSet<>();
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> usedTags.addAll(op.getTags())));

            List<Tag> tags = new ArrayList<>();
            tagDescriptions.forEach((name, description) -> {
                if (usedTags.contains(name)) {
                    tags.add(new Tag().name(name).description(description));
                }
            });
            openApi.setTags(tags);
        };
    }

    private String resolveTag(String path) {
        if (path.startsWith("/api/auth")) return "Authentication";
        if (path.startsWith("/api/checkout")) return "Checkout";
        if (path.startsWith("/api/webhook")) return "Webhook";
        if (path.startsWith("/api/orders")) return "Order";
        if (path.startsWith("/api/product-category")) return "Product Category";
        if (path.startsWith("/api/products")) return "Product";
        if (path.startsWith("/api/countries")) return "Country";
        if (path.startsWith("/api/states")) return "State";
        if (path.startsWith("/api/customers")) return "Customer";
        if (path.startsWith("/api/profile")) return "API Profile";
        return "Other";
    }

}
