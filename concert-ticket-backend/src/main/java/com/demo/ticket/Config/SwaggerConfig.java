// 閱讀導引：依登入、管理、活動與訂票路徑建立四個 API 文件分組，供 Swagger UI 查閱。
package com.demo.ticket.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 建立登入相關 API 的 Swagger 文件分組。
     */
    @Bean
    public GroupedOpenApi loginApi() {
        return GroupedOpenApi.builder()
                .group("A-login")
                .pathsToMatch("/v1/login/**")
                .build();
    }

    /**
     * 建立管理相關 API 的 Swagger 文件分組。
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("B-admin")
                .pathsToMatch("/v1/admin/**")
                .build();
    }

    /**
     * 建立活動相關 API 的 Swagger 文件分組。
     */
    @Bean
    public GroupedOpenApi activityApi() {
        return GroupedOpenApi.builder()
                .group("C-activity")
                .pathsToMatch("/v1/activity/**")
                .build();
    }

    /**
     * 建立訂票相關 API 的 Swagger 文件分組。
     */
    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("D-booking")
                .pathsToMatch("/v1/booking/**")
                .build();
    }

}