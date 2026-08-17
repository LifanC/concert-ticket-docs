package com.demo.ticket.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi loginApi() {
        return GroupedOpenApi.builder()
                .group("A-login")
                .pathsToMatch("/v1/login/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("B-admin")
                .pathsToMatch("/v1/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi activityApi() {
        return GroupedOpenApi.builder()
                .group("C-activity")
                .pathsToMatch("/v1/activity/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("D-booking")
                .pathsToMatch("/v1/booking/**")
                .build();
    }

}