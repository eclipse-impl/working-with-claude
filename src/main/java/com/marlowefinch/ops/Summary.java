package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The stand-up line: the four KPIs plus the carrier with the lowest on-time rate and the
 * ticket category with the most tickets. Both names are null when the range has no data.
 */
public record Summary(
        LocalDate from,
        LocalDate to,
        Double onTimeRate,
        long openTickets,
        BigDecimal revenue,
        long orders,
        String worstCarrier,
        String busiestTicketCategory) {
}
