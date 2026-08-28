package com.demo.ticket.Dto.Booking;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "seat_id",
                "token",
        }
)
@Schema(description = "座位資料")
public class BookingSelectOnlySeatsRequest {

    @Schema(
            description = "座位編號",
            example = "AF-10",
            minLength = 5,
            maxLength = 5,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "座位編號不可為空")
    @Size(min = 5, max = 5, message = "座位編號長度需為 5 個字元")
    @Pattern(
            regexp = "^[A-Z]{2}-\\d{2}$",
            message = "座位編號格式需為 XX-00，例如 AF-10"
    )
    private String seat_id;

    private String token;

    public String getSeat_id() {
        return seat_id;
    }

    public void setSeat_id(String seat_id) {
        this.seat_id = seat_id;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}

















