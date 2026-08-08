package com.demo.ticket.Dto.Booking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "orderno",
                "email",
                "status",
        }
)
@Schema(description = "取消訂單")
public class BookingCanceTicketRequest {

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
            description = "狀態",
            example = "狀態只能為 已成立、待付款",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "狀態不可為空")
    @Pattern(
            regexp = "^(已成立|待付款)$",
            message = "狀態只能為 已成立、待付款"
    )
    private String status;

    private String token;

    public String getOrderno() {
        return orderno;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
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

















