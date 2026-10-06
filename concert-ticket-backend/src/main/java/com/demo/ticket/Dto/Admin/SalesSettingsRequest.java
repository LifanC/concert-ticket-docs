package com.demo.ticket.Dto.Admin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@JsonPropertyOrder({"version", "maxTicketsPerMember", "zones", "ticketTypes"})
@Schema(description = "儲存場次分區、票種定價及限購設定")
public record SalesSettingsRequest(
    @Schema(
            description = "設定版本，首次設定為 0，修改時使用查詢取得的版本",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "設定版本不可為空")
    @PositiveOrZero(message = "設定版本不可小於 0")
    Long version,

    @Schema(
            description = "每位會員於此場次的限購張數，包含有效待付款訂單",
            example = "4",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "限購張數不可為空")
    @Min(value = 1, message = "限購張數必須介於 1～10000")
    @Max(value = 10000, message = "限購張數必須介於 1～10000")
    Integer maxTicketsPerMember,

    @Schema(
            description = "場次分區，必須涵蓋全部座位且不得重疊",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "至少需設定一個分區")
    @Size(max = 150, message = "分區數量不可超過 150 個")
    List<@NotNull(message = "分區不可為空") @Valid ZoneRequest> zones,

    @Schema(
            description = "場次可購買的票種",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "至少需設定一個票種")
    @Size(max = 30, message = "票種數量不可超過 30 個")
    List<@NotNull(message = "票種不可為空") @Valid TicketTypeRequest> ticketTypes
) {}
