package com.demo.ticket.Dto.Booking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "activity_id",
                "selected_date",
                "selected_session",
        }
)
@Schema(description = "指定場次資料")
public record BookingSelectOnlyUnavailableSeatsRequest(

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
        String activity_id,

        @Schema(
                description = "活動日期",
                example = "2026-08-15",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(
                regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
                message = "活動日期格式需為 yyyy-MM-dd"
        )
        String selected_date,

        @Schema(
                description = "場次時間",
                example = "19:30",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(
                regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                message = "場次時間格式需為 HH:mm"
        )
        String selected_session

) {}
