package com.example.IncidentPulse.ApplicationCofig;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI is served at:
 *   - JSON spec : /v3/api-docs
 *   - Swagger UI: /swagger-ui.html  (redirects to /swagger-ui/index.html)
 *
 * The "bearerAuth" scheme below makes the Authorize button in the UI work
 * with the JWT issued by /api/v1/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI incidentPulseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IncidentPulse API")
                        .description("Incident management REST API: auth, users, on-call shifts, incidents, and audit history.")
                        .version("v1")
                        .contact(new Contact().name("IncidentPulse").url("https://github.com/")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
