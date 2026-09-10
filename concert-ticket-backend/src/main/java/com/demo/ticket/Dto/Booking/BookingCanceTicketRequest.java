package com.demo.ticket.Dto.Booking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "orderno",
                "session_id",
                "status",
        }
)
@Schema(description = "取消訂單")
public record BookingCanceTicketRequest(

    @Schema(
            description = "訂單編號",
            example = "CT202608151",
            minLength = 13,
            maxLength = 13,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "訂單編號不可為空")
    @Pattern(
            regexp = "^CT[0-9]{8}[0-9]+$",
            message = "訂單編號格式需為 CT＋八位日期＋數字序號，例如 CT202608151"
    )
    String orderno,

    @Schema(
            description = "場次編號",
            example = "S-20260801-001",
            minLength = 14,
            maxLength = 14,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "場次編號不可為空")
    @Size(min = 14, max = 14, message = "場次編號長度需為 14 個字元")
    @Pattern(
            regexp = "^S-\\d{8}-\\d{3}$",
            message = "場次編號格式需為 S-YYYYMMDD-NNN，例如 S-20260801-001"
    )
    String session_id,

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
    String status

) {}

















