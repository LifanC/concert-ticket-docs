package com.demo.ticket.Dto.Login;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "email",
                "token",
        }
)
@Schema(description = "登出")
public class LoginLogoutRequest {

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

        private String token;

        public String getEmail() {
                return email;
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
