package com.demo.ticket.Dto.Admin;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "id",
                "token",
        }
)
@Schema(description = "刪除活動")
public class AdminDeleteActivityRequest {

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
    private String id;

    private String token;

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}
