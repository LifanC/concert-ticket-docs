// 閱讀導引：應用程式啟動時確保資料庫已有當月 JWT 簽章祕密；祕密建立與讀取由 SecretMapper 負責。
package com.demo.ticket.Config.log;

import com.demo.ticket.Mapper.SecretMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@Transactional
public class SecretKey {

    private final Logger logger = LoggerFactory.getLogger(SecretKey.class);

    @Resource
    private SecretMapper secretMapper;

    /**
     * 啟動時檢查當月祕密；不存在時產生 64 位元組隨機資料，以 Base64 字串保存，已有資料則沿用。
     */
    @PostConstruct
    public void getSecret() {
        boolean cnt = secretMapper.getSecret();
        if (cnt) {
            logger.info("SecretKey 資料已存在");
        } else {
            SecureRandom random = new SecureRandom();
            // 實際產生 64 bytes（512 bits）的隨機資料，再編碼為可存入資料庫的 Base64 文字。
            byte[] bytes = new byte[64];
            random.nextBytes(bytes);
            String secretNum = Base64.getEncoder().encodeToString(bytes);
            secretMapper.createSecret(secretNum);
        }

    }

}
