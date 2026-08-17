package com.demo.ticket.Dto.Booking;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "name",
                "date",
                "time",
                "token",
        }
)
@Schema(description = "售賣日期")
public class BookingSessionSalesDateRequest {

    @Schema(
            description = "活動名稱",
            example = "夏日星光音樂祭",
            minLength = 2,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動名稱不可為空")
    @Size(min = 2, max = 50, message = "活動名稱長度需介於 2~50 字")
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5A-Za-z ]+$",
            message = "活動名稱格式錯誤"
    )
    private String name;

    @Schema(
            description = "活動日期",
            example = "2026-08-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "活動日期格式需為 yyyy-MM-dd"
    )
    private String date;

    @Schema(
            description = "場次時間",
            example = "19:30",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "場次時間格式需為 HH:mm"
    )
    private String time;

    private String token;

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}

















