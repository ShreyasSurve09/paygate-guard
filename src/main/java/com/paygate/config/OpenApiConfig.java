package com.paygate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayGate Guard API Engine")
                        .version("1.0.0")
                        .description("High-reliability idempotent payment webhook ingestion engine with exponential retries and dead-letter queue recovery.")
                        .contact(new Contact()
                                .name("PayGate Guard Engineering")
                                .email("dev@paygate.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
