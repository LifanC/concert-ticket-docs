// 閱讀導引：共用格式轉換工具，將資料轉成其他流程所需的格式。
package com.demo.ticket.Common;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@NullMarked
public final class ConvertFormat {

    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final String BEARER_PREFIX = "Bearer ";

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private static final DateTimeFormatter INPUT_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(
                            ChronoField.NANO_OF_SECOND,
                            0,
                            9,
                            true
                    )
                    .optionalEnd()
                    .toFormatter();

    private ConvertFormat() {
    }

    /** 取得台北時區的現在時間，格式為 yyyy-MM-dd HH:mm:ss。 */
    public static String now() {
        return LocalDateTime.now(TAIPEI_ZONE).format(OUTPUT_FORMAT);
    }

    /**
     * 將日期文字轉為台北時區的 yyyy-MM-dd HH:mm:ss；空白輸入回傳現在時間，Z 結尾輸入視為 UTC。
     */
    public static String time(@Nullable String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return now();
        }

        LocalDateTime dateTime;

        // UTC 日期先轉成台北時間；沒有時區的日期文字則依原本本地時間處理。
        if (dateText.endsWith("Z")) {
            dateTime = Instant.parse(dateText)
                    .atZone(TAIPEI_ZONE)
                    .toLocalDateTime();
        } else {
            dateTime = LocalDateTime.parse(dateText, INPUT_FORMAT);
        }

        return dateTime.format(OUTPUT_FORMAT);
    }

    /**
     * 從 Bearer 標頭取出 token；標頭缺值或格式不符時回傳 null。
     */
    public static @Nullable String resolveToken(@Nullable String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

}
