// 閱讀導引：共用格式轉換工具，將資料轉成其他流程所需的格式。
package com.demo.ticket.Common;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class ConvertFormat {

    private static final ZoneId ZONE = ZoneId.of("Asia/Taipei");

    private static final DateTimeFormatter OUT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FLEXIBLE =
            new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(
                            ChronoField.NANO_OF_SECOND,
                            0,
                            9,
                            true)
                    .optionalEnd()
                    .toFormatter();

    /**
     * 將日期文字轉為台北時區的 yyyy-MM-dd HH:mm:ss；空白輸入回傳現在時間，Z 結尾輸入視為 UTC。
     */
    public static String time(String str) {

        if (!StringUtils.hasText(str)) {
            return LocalDateTime.now(ZONE).format(OUT);
        }

        LocalDateTime ldt;

        // ✅ 1. ISO UTC
        if (str.endsWith("Z")) {
            ldt = Instant.parse(str)
                    .atZone(ZONE)
                    .toLocalDateTime();
        }
        // ✅ 2. 本地格式
        else {
            ldt = LocalDateTime.parse(str, FLEXIBLE);
        }

        return ldt.format(OUT);

    }

    /**
     * 從 Bearer 標頭取出 token；標頭缺值或格式不符時回傳 null。
     */
    public static String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        } else {
            return null;
        }
    }

}
