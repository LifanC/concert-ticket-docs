package com.demo.ticket.Dto.Booking;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "activity_id",
                "token",
        }
)
@Schema(description = "單一場次金額")
public class BookingSelectOnlyActivitiesPriceRequest {

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

    private String token;

    public String getActivity_id() {
        return activity_id;
    }

    public void setActivity_id(String activity_id) {
        this.activity_id = activity_id;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}

















