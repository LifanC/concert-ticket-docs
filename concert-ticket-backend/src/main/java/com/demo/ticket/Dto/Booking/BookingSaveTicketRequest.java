package com.demo.ticket.Dto.Booking;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder(
        {
                "session_id",
                "activity_id",
                "name",
                "date",
                "time",
                "status",
                "total",
                "token",
        }
)
@Schema(description = "新增訂單")
public class BookingSaveTicketRequest {

    @Schema(
            description = "場次編號",
            example = "S-001",
            minLength = 5,
            maxLength = 5,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "場次編號不可為空")
    @Size(min = 5, max = 5, message = "場次編號長度需為 5 個字元")
    @Pattern(
            regexp = "^S-\\d{3}$",
            message = "場次編號格式需為 S-NNN，例如 S-001"
    )
    private String session_id;

    @Schema(
            description = "活動編號",
            example = "ACT-2026-001",
            minLength = 12,
            maxLength = 12,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動編號不可為空")
    @Size(min = 12, max = 12, message = "活動編號長度需為 12 個字元")
    @Pattern(
            regexp = "^ACT-\\d{4}-\\d{3}$",
            message = "活動編號格式需為 ACT-YYYY-NNN，例如 ACT-2026-001"
    )
    private String activity_id;

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

    @Schema(
            description = "狀態",
            example = "狀態只能為 等待付款",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "狀態不可為空")
    @Pattern(
            regexp = "^(PENDING_PAYMENT)$",
            message = "狀態只能為 等待付款"
    )
    private String status;

    @Schema(
            description = "票價",
            example = "1280",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "票價不可為空")
    @DecimalMin(value = "0", message = "票價不可小於 0")
    @Digits(integer = 10, fraction = 0, message = "票價必須為整數")
    private BigDecimal price;

    private String token;

    public String getSession_id() {
        return session_id;
    }

    public String getActivity_id() {
        return activity_id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}

















