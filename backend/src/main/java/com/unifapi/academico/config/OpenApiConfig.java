package com.unifapi.academico.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Academico+ API")
                .version("1.0.0")
                .description("API REST para Plataforma de Networking Acadêmico")
                .contact(new Contact()
                    .name("UNIFAPI")
                    .email("dev@unifapi.edu.br")
                    .url("https://unifapi.edu.br")
                )
            );
    }
}