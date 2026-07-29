package com.segurosbolivar.policy.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Seguros Bolivar Policy Management API",
                version = "1.0.0",
                description = "Rental policy and risk lifecycle API"
        ),
        security = @SecurityRequirement(name = "apiKey")
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "x-api-key",
        description = "Assessment key: 123456. Externalize it outside local development."
)
public class OpenApiConfiguration {
}

