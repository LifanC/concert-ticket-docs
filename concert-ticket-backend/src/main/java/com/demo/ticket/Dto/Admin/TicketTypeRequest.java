package com.demo.ticket.Dto.Admin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonPropertyOrder({"name", "pricePercent", "eligibility"})
@Schema(description = "場次票種與價格比例")
public record TicketTypeRequest(
    @Schema(
            description = "票種名稱",
            example = "學生票",
            maxLength = 40,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "票種名稱不可為空")
    @Size(max = 40, message = "票種名稱不可超過 40 個字元")
    String name,

    @Schema(
            description = "分區基準票價的百分比，100 為全票、50 為半價",
            example = "50.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "票種價格比例不可為空")
    @DecimalMin(value = "0.01", message = "票種價格比例必須介於 0.01～100")
    @DecimalMax(value = "100", message = "票種價格比例必須介於 0.01～100")
    @Digits(integer = 3, fraction = 2, message = "票種價格比例最多為 3 位整數與 2 位小數")
    BigDecimal pricePercent,

    @Schema(
            description = "購票資格說明",
            example = "入場時需出示有效學生證",
            maxLength = 200,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 200, message = "購票資格說明不可超過 200 個字元")
    String eligibility
) {}
