package com.demo.ticket.Dto.Admin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder(
        {
                "token",
        }
)
@Schema(description = "token")
public class AdminRequest {

    private String token;

    public void setAuthHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if ("Bearer".equals(token.trim())) {
            throw new RuntimeException("Token 不可為空");
        }
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
