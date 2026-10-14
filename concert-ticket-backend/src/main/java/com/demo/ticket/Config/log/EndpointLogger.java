// 閱讀導引：啟動時輸出本機 Swagger UI 與 API 文件網址，方便開發者開啟文件。
package com.demo.ticket.Config.log;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EndpointLogger implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EndpointLogger.class);

    /**
     * 應用程式啟動後輸出 Swagger UI 與 API 文件網址。
     */
    @Override
    public void run(@Nonnull String... args) {
        logger.info("""
                                
                                \tLocal:      http://localhost:8080/api/swagger-ui/index.html
                                \tLocal:      http://localhost:8080/api/v3/api-docs
                                ----------------------------------------------------------"""
        );
    }
}

