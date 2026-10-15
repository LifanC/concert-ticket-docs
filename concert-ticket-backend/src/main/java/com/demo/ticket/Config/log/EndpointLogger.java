// 閱讀導引：啟動時輸出本機 Swagger UI 與 API 文件網址，方便開發者開啟文件。
package com.demo.ticket.Config.log;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Nonnull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EndpointLogger implements CommandLineRunner {

    /**
     * 應用程式啟動後輸出 Swagger UI 與 API 文件網址。
     */
    @Override
    public void run(@Nonnull String... args) {
        log.info("""
                                
                                \tLocal:      http://localhost:8080/api/swagger-ui/index.html
                                \tLocal:      http://localhost:8080/api/v3/api-docs
                                ----------------------------------------------------------"""
        );
    }
}

