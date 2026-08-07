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
                "name",
                "email",
                "phone",
                "password",
        }
)
@Schema(description = "註冊")
public class RegisterRequest {

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
    private String name;

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
            description = "使用者手機號碼不可為空",
            example = "0912345678",
            minLength = 6,
            maxLength = 20,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(min = 6, max = 20, message = "手機號碼長度需介於 6~20 字")
    @Pattern(
            regexp = "^[0-9+\\-() ]+$",
            message = "手機號碼包含不允許的字元"
    )
    private String phone;

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
    private String password;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }
}
