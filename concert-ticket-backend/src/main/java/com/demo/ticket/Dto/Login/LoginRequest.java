package com.demo.ticket.Dto.Login;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "account",
                "password",
        }
)
@Schema(description = "登入")
public class LoginRequest {

    @Schema(
            description = "電子信箱或帳號",
            example = "wang@example.com",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "電子信箱或帳號不可為空")
    @Size(max = 100, message = "電子信箱或帳號不可超過 100 字")
    @Pattern(
            regexp = "^(?:[\\u4e00-\\u9fa5A-Za-z]{2,100}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$",
            message = "請輸入電子信箱或帳號"
    )
    private String account;

    @Schema(
            description = "使用者密碼",
            example = "321ewqdsacxz",
            minLength = 8,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, max = 100, message = "密碼長度需介於 8~100 字")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "密碼需包含英文與數字"
    )
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public String getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }
}
