package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonPropertyOrder(
        {
                "id",
                "name",
                "category",
                "date",
                "venue",
                "status",
                "price",
                "description",
        }
)
@Schema(description = "增加、修改活動")
public class AdminSaveActivityRequest {

    @Schema(
            description = "活動編號",
            example = "ACT-2026-001",
            minLength = 12,
            maxLength = 12,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "活動編號不可為空")
    @Size(min = 12, max = 12, message = "活動編號長度需為 12 個字元")
    @Pattern(
            regexp = "^ACT-\\d{4}-\\d{3}$",
            message = "活動編號格式需為 ACT-YYYY-NNN，例如 ACT-2026-001"
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
            regexp = "^(音樂演唱會|舞台劇|展覽特展)$",
            message = "活動類型只能為 音樂演唱會、舞台劇、展覽特展"
    )
    private String category;

    @Schema(
            description = "活動日期",
            example = "2026-08-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$",
            message = "活動日期格式需為 yyyy-MM-dd"
    )
    private String date;

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
            description = "狀態",
            example = "狀態只能為 即將開賣、售票中、已結束",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "狀態不可為空")
    @Pattern(
            regexp = "^(即將開賣|售票中|已結束)$",
            message = "狀態只能為 即將開賣、售票中、已結束"
    )
    private String status;

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

    public String getDate() {
        return date;
    }

    public String getVenue() {
        return venue;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
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
