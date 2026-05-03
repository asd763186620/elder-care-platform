package com.eldercare.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 3 / OpenAPI 公共配置，所有业务服务都会通过 common 模块复用。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 文档基础信息。
     *
     * @param applicationName 当前 Spring 应用名称。
     * @return OpenAPI 配置对象。
     */
    @Bean
    public OpenAPI elderCareOpenAPI(@Value("${spring.application.name:elder-care-service}") String applicationName) {
        // 定义 JWT 鉴权方案名称，Swagger UI 会用它展示 Authorize 按钮。
        String securitySchemeName = "BearerAuth";
        // 构造并返回 OpenAPI 文档对象。
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("社区养老服务预约小程序后端接口文档")
                        .version("0.0.1")
                        .contact(new Contact().name("elder-care-platform")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .schemaRequirement(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}
