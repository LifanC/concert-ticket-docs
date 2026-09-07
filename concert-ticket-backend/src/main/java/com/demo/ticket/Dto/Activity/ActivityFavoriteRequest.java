package com.demo.ticket.Dto.Activity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "activity_id",
                "session_id",
        }
)
@Schema(description = "收藏活動")
public record ActivityFavoriteRequest(

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
    String session_id

) {}
