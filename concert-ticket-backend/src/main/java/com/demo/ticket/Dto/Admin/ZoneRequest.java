package com.demo.ticket.Dto.Admin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder({"name", "color", "rowStart", "rowEnd", "price"})
@Schema(description = "場次分區定價")
public record ZoneRequest(
    @Schema(
            description = "分區名稱",
            example = "前區",
            maxLength = 40,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "分區名稱不可為空")
    @Size(max = 40, message = "分區名稱不可超過 40 個字元")
    String name,

    @Schema(
            description = "分區顏色，使用十六進位色碼",
            example = "#409EFF",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "分區顏色不可為空")
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "分區顏色格式需為 #RRGGBB，例如 #409EFF"
    )
    String color,

    @Schema(
            description = "分區起始排別",
            example = "A",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "分區起始排別不可為空")
    @Pattern(
            regexp = "^[A-Z]{1,2}$",
            message = "分區起始排別需為 1～2 個大寫英文字母"
    )
    String rowStart,

    @Schema(
            description = "分區結束排別，包含此排",
            example = "D",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "分區結束排別不可為空")
    @Pattern(
            regexp = "^[A-Z]{1,2}$",
            message = "分區結束排別需為 1～2 個大寫英文字母"
    )
    String rowEnd,

    @Schema(
            description = "分區基準票價",
            example = "1280.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "分區票價不可為空")
    @DecimalMin(value = "0", message = "分區票價不可小於 0")
    @Digits(integer = 10, fraction = 2, message = "分區票價最多為 10 位整數與 2 位小數")
    BigDecimal price
) {}
