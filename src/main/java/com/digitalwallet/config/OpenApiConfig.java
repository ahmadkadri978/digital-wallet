package com.digitalwallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI digitalWalletOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Wallet API")
                        .description("API documentation for the Digital Wallet system")
                        .version("1.0.0"));
    }
}