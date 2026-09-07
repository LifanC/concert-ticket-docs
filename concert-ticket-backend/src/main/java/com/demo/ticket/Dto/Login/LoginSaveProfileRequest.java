package com.demo.ticket.Dto.Login;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonPropertyOrder(
        {
                "name",
                "phone",
                "birthday",
        }
)
@Schema(description = "修改會員資料")
public record LoginSaveProfileRequest(

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
    String phone,

    @Schema(
            description = "使用者生日",
            example = "1911-01-01",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "生日格式需為 yyyy-MM-dd"
    )
    String birthday

) {}
