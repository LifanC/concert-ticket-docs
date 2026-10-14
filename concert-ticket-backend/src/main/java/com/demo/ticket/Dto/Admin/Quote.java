// 閱讀導引：封裝後端計算的票價與分區、票種資訊，建立訂單時保存這份計價結果。
package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

public record Quote(
        BigDecimal price,
        String zoneId,
        String ticketTypeId,
        String zoneName,
        String ticketTypeName) {
}
