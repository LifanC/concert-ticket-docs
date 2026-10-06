package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

public record Quote(
        BigDecimal price,
        String zoneId,
        String ticketTypeId,
        String zoneName,
        String ticketTypeName) {
}
