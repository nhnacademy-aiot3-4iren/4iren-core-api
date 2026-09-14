package com.nhnacademy.core.config;

import com.nhnacademy.core.exception.response.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI coreOpenApi() {
        Components components = new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        ModelConverters.getInstance()
                .read(ErrorResponse.class)
                .forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("4iren Core API")
                        .description("팀, 건물, 공간, 기기, 센서 및 대시보드 관리 API")
                        .version("v1"))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OperationCustomizer standardApiResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            operation.getResponses().forEach((status, response) -> {
                if (!status.startsWith("2") && !status.equals("401")) {
                    response.setContent(errorResponseContent());
                }
            });
            operation.getResponses().addApiResponse(
                    "401",
                    new ApiResponse().description("JWT가 없거나 유효하지 않음 (Gateway에서 빈 본문으로 반환)")
            );
            operation.getResponses().addApiResponse(
                    "default",
                    new ApiResponse()
                            .description("Core API 오류")
                            .content(errorResponseContent())
            );

            return operation;
        };
    }

    private Content errorResponseContent() {
        return new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
        );
    }
}
