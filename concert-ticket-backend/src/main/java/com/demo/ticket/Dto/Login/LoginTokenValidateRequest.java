package com.demo.ticket.Dto.Login;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "account",
        }
)
@Schema(description = "驗證")
public class LoginTokenValidateRequest {

    @Schema(
            description = "帳號",
            example = "wang",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "帳號不可為空")
    @Size(max = 100, message = "帳號不可超過 100 字")
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5A-Za-z]{2,100}$",
            message = "請輸入帳號"
    )
    private String account;

    public String getAccount() {
        return account;
    }

}
