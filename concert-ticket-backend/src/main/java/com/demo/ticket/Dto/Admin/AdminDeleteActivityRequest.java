package com.demo.ticket.Dto.Admin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "id",
        }
)
@Schema(description = "刪除活動")
public class AdminDeleteActivityRequest {

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
    private String id;

    private String token;

    public String getId() {
        return id;
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
