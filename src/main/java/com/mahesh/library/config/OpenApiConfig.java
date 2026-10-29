package com.mahesh.library.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {
 // "bearerAuth" adds Swagger UI's Authorize button; paste the accessToken from /auth/login there.
 @Bean OpenAPI libraryOpenAPI(){ return new OpenAPI().info(new Info().title("Library Management API").version("v1").description("Library catalogue, members, borrowing and returns, secured with JWT"))
   .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
   .addSecurityItem(new SecurityRequirement().addList("bearerAuth")); }
}
