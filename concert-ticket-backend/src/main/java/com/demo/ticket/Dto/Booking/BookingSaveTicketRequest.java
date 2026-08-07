package com.demo.ticket.Dto.Booking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder(
        {
                "orderno",
                "email",
                "name",
                "date",
                "time",
                "status",
                "total",
        }
)
@Schema(description = "新增訂單")
public class BookingSaveTicketRequest {

    @Schema(
            description = "訂單編號",
            example = "CT20260815001",
            minLength = 13,
            maxLength = 13,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "訂單編號不可為空")
    @Size(min = 13, max = 13, message = "訂單編號長度需為 13 個字元")
    @Pattern(
            regexp = "^CT\\d{4}\\d{2}\\d{2}\\d{3}$",
            message = "訂單編號格式需為 CTYYYYMMDDNNN，例如 CT20260815001"
    )
    private String orderno;

    @Schema(
            description = "電子信箱",
            example = "wang@example.com",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "電子信箱不可為空")
    @Email(message = "電子信箱格式錯誤")
    @Size(max = 100, message = "電子信箱不可超過 100 字")
    private String email;

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
            example = "狀態只能為 已成立",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "狀態不可為空")
    @Pattern(
            regexp = "^(已成立|已取消)$",
            message = "狀態只能為 已成立、已取消"
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

    public String getOrderno() {
        return orderno;
    }

    public String getEmail() {
        return email;
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
        String token = authHeader.replace("Bearer ", "");
        if ("Bearer".equals(token.trim())) {
            throw new RuntimeException("Token 不可為空");
        }
        this.token = token;
    }
}

















