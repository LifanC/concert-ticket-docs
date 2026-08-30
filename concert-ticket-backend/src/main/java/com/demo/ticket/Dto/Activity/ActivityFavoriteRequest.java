package com.demo.ticket.Dto.Activity;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonPropertyOrder(
        {
                "activity_id",
                "token",
        }
)
@Schema(description = "收藏活動")
public class ActivityFavoriteRequest {

    @Schema(description = "活動編號", example = "ACT-2026-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活動編號不可為空")
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
