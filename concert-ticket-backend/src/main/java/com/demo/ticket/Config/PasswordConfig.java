// 閱讀導引：提供密碼編碼器，供註冊儲存密碼及登入比對密碼使用。
package com.demo.ticket.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * 建立 BCrypt 密碼編碼器，供密碼儲存與比對共用。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
