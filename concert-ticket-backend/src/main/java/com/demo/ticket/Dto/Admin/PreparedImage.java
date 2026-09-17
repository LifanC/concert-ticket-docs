package com.demo.ticket.Dto.Admin;

public record PreparedImage(
        int width,
        int height,
        byte[] data
) {}
