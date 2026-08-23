package com.demo.ticket.Dto.Admin;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder(
        {
                "id",
                "activity",
                "date",
                "time",
                "salesdate",
                "salestime",
                "capacity",
                "sold",
                "token",
        }
)
@Schema(description = "新增場次")
public class AdminCreateSessionRequest {

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
    private String id;

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
    private String activity;

    @Schema(
            description = "場次日期",
            example = "2026-08-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "場次日期格式需為 yyyy-MM-dd"
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
            description = "開賣日期",
            example = "2026-08-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "場次日期格式需為 yyyy-MM-dd"
    )
    private String salesdate;

    @Schema(
            description = "開賣時間",
            example = "19:30",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "場次時間格式需為 HH:mm"
    )
    private String salestime;

    @Schema(
            description = "可售座位數",
            example = "458",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "可售座位數不可為空")
    @DecimalMin(value = "0", message = "可售座位數不可小於 0")
    @Digits(integer = 10, fraction = 0, message = "可售座位數必須為整數")
    private BigDecimal capacity;

    @Schema(
            description = "已售數",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "已售數不可為空")
    @DecimalMin(value = "0", message = "已售數必須為 0")
    @DecimalMax(value = "0", message = "已售數必須為 0")
    @Digits(integer = 10, fraction = 0, message = "已售數必須為整數")
    private BigDecimal sold;

    private String token;

    public String getId() {
        return id;
    }

    public String getActivity() {
        return activity;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getSalesdate() {
        return salesdate;
    }

    public String getSalestime() {
        return salestime;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public BigDecimal getSold() {
        return sold;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}
