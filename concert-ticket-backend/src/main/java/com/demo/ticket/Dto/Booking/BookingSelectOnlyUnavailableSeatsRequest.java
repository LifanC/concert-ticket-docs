package com.demo.ticket.Dto.Booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BookingSelectOnlyUnavailableSeatsRequest(
        @NotBlank @Pattern(regexp = "^S-\\d{8}-\\d{3}$") String session_id
) {}
