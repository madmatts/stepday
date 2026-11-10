package pl.stepday.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: /api/swagger-ui.html, specyfikacja: /api/v3/api-docs. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI stepdayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stepday API")
                        .version("0.1.0")
                        .description("API pod ekrany z makiet (dokument „Stepday: warstwa danych”, sekcja 7). "
                                + "Wszystkie zapisy są idempotentne; stan dnia liczy serwer."))
                // Ścieżka względna: działa i przez nginx (:8000/api), i bezpośrednio (:8080/api).
                .addServersItem(new Server().url("/api"))
                .components(new Components().addSecuritySchemes("basic",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basic"));
    }
}
