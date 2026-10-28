package com.mahesh.library.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {
 @Bean OpenAPI libraryOpenAPI(){ return new OpenAPI().info(new Info().title("Library Management API").version("v1").description("Library catalogue, members, borrowing and returns")); }
}