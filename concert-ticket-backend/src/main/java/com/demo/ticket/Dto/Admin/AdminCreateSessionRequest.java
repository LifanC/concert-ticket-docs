package com.demo.ticket.Dto.Admin;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder(
        {
                "id",
                "activity_id",
                "date",
                "time",
                "salesdate",
                "salestime",
                "status",
                "token",
        }
)
@Schema(description = "新增場次")
public class AdminCreateSessionRequest {

    @Schema(
            description = "場次編號",
            example = "S-20260801-001",
            minLength = 14,
            maxLength = 14,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Pattern(
            regexp = "^$|^S-\\d{8}-\\d{3}$",
            message = "場次編號格式需為 S-YYYYMMDD-NNN，例如 S-20260801-001"
    )
    private String id;

    @Schema(
            description = "活動編號",
            example = "ACT-20260801-001",
            minLength = 16,
            maxLength = 16,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動編號不可為空")
    @Size(min = 16, max = 16, message = "活動編號長度需為 16 個字元")
    @Pattern(
            regexp = "^ACT-\\d{8}-\\d{3}$",
            message = "活動編號格式需為 ACT-YYYYMMDD-NNN，例如 ACT-20260801-001"
    )
    private String activity_id;

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
            description = "狀態",
            example = "狀態只能為 即將開賣、售票中、已售完、已結束",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "狀態不可為空")
    @Pattern(
            regexp = "^(COMING_SOON|TICKETS_ARE_ON_SALE|SOLD_OUT|ENDED)$",
            message = "狀態只能為 即將開賣、售票中、已售完、已結束"
    )
    private String status;

    private String token;

    public String getId() {
        return id;
    }

    public String getActivity_id() {
        return activity_id;
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

    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}
