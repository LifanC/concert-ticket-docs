package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

import com.demo.ticket.Common.ConvertFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "id",
                "name",
                "category",
                "venue",
                "price",
                "description",
                "column",
                "row",
                "token",
        }
)
@Schema(description = "增加、修改活動")
public class AdminSaveActivityRequest {

    @Schema(
            description = "活動編號",
            example = "ACT-20260801-001",
            minLength = 16,
            maxLength = 16,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Pattern(
            regexp = "^$|^ACT-\\d{8}-\\d{3}$",
            message = "活動編號格式需為 ACT-YYYYMMDD-NNN，例如 ACT-20260801-001"
    )
    private String id;

    @Schema(
            description = "活動名稱",
            example = "夏日星光音樂祭",
            minLength = 2,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動名稱不可為空")
    @Size(min = 2, max = 50, message = "活動名稱長度需介於 2~50 字")
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5A-Za-z ]+$",
            message = "活動名稱格式錯誤"
    )
    private String name;

    @Schema(
            description = "活動類型",
            example = "活動類型只能為 音樂演唱會、舞台劇、展覽特展",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動類型不可為空")
    @Pattern(
            regexp = "^(MUSIC_CONCERT|STAGE_PLAY|SPECIAL_EXHIBITION)$",
            message = "活動類型只能為 音樂演唱會、舞台劇、展覽特展"
    )
    private String category;

    @Schema(
            description = "場地名稱",
            example = "台北流行音樂中心",
            minLength = 2,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動場地名稱不可為空")
    @Size(min = 2, max = 50, message = "活動場地名稱長度需介於 2~50 字")
    @Pattern(
            regexp = "^[\\u4e00-\\u9fa5A-Za-z ]+$",
            message = "活動場地名稱格式錯誤"
    )
    private String venue;

    @Schema(
            description = "票價",
            example = "1280",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "票價不可為空")
    @DecimalMin(value = "0", message = "票價不可小於 0")
    @Digits(integer = 10, fraction = 0, message = "票價必須為整數")
    private BigDecimal price;

    @Schema(
            description = "活動說明",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String description;

    @Schema(
            description = "欄",
            example = "AB",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "欄不可為空")
    @Size(min = 2, max = 2, message = "欄長度需介於 2~50 字")
    @Pattern(
            regexp = "^[A-Z]{2}$",
            message = "欄格式錯誤"
    )
    private String column;

    @Schema(
            description = "列",
            example = "1~10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "列不能為空")
    @Min(value = 1, message = "列必須介於 1～10")
    @Max(value = 10, message = "列必須介於 1～10")
    private BigDecimal row;

    private String token;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getVenue() {
        return venue;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getColumn() {
        return column;
    }

    public BigDecimal getRow() {
        return row;
    }

    public String getToken() {
        return token;
    }

    public void setAuthHeader(String authHeader) {
        this.token = ConvertFormat.resolveToken(authHeader);
    }
}
