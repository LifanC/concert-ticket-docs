package com.demo.ticket.Exception;

import org.springframework.http.HttpStatus;

public class BookingException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BookingException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static void requireOne(int rows) {
        if (rows != 1) {
            throw new BookingException("INVENTORY_CONSISTENCY_ERROR",
                    "庫存更新失敗，交易已取消", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
