package com.bikestore.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bikes Asaro API",
                description = "REST API for the Bikes Asaro e-commerce platform. It provides endpoints for catalog management, authentication, user management, and file uploads.",
                version = "1.0",
                contact = @Contact(
                        name = "Pablo Salom Pita",
                        email = "pablosalompita@gmail.com",
                        url = "https://github.com/salompablo"
                )
        ),
        servers = {
                @Server(description = "Local Environment", url = "http://localhost:8080")
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Enter the JWT token obtained after authenticating",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {
}