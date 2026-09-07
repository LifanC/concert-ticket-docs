package com.demo.ticket.Dto.Login;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "account",
                "name",
                "email",
                "phone",
                "password",
        }
)
@Schema(description = "註冊")
public record RegisterRequest(

        @Schema(
                description = "使用者帳號",
                example = "JohnSmith123",
                minLength = 2,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "帳號不可為空")
        @Size(min = 2, max = 50, message = "帳號長度需介於 2~50 字")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9]*$",
                message = "帳號格式錯誤"
        )
        String account,

        @Schema(
                description = "使用者姓名",
                example = "王小明",
                minLength = 2,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "姓名不可為空")
        @Size(min = 2, max = 50, message = "姓名長度需介於 2~50 字")
        @Pattern(
                regexp = "^[\\u4e00-\\u9fa5A-Za-z ]+$",
                message = "姓名格式錯誤"
        )
        String name,

        @Schema(
                description = "電子信箱",
                example = "wang@example.com",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "電子信箱不可為空")
        @Email(message = "電子信箱格式錯誤")
        @Size(max = 100, message = "電子信箱不可超過 100 字")
        String email,

        @Schema(
                description = "使用者手機號碼，可不填；填寫時需為 10 個數字",
                example = "0912345678",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(
                regexp = "^$|^\\d{10}$",
                message = "手機號碼需為 10 個數字"
        )
        String phone,

        @Schema(
                description = "使用者密碼",
                example = "Abcd1234!",
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
        String password

) {}
