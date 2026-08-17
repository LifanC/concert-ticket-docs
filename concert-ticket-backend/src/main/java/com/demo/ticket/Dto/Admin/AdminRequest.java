package com.demo.ticket.Dto.Admin;

import com.demo.ticket.Common.ConvertFormat;
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
        this.token = ConvertFormat.resolveToken(authHeader);
    }

    public String getToken() {
        return token;
    }
}
