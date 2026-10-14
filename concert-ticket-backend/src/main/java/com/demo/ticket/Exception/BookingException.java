// 閱讀導引：攜帶訂票錯誤代碼與 HTTP 狀態；資料更新筆數不符合預期時中止流程。
package com.demo.ticket.Exception;

import org.springframework.http.HttpStatus;

public class BookingException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    /**
     * 建立帶有業務錯誤代碼、訊息及 HTTP 狀態的執行期例外；交易內拋出時可觸發回滾。
     */
    public BookingException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * 回傳錯誤代碼。
     */
    public String getCode() { return code; }
    /**
     * 回傳此錯誤應使用的 HTTP 狀態，不是訂單的業務狀態。
     */
    public HttpStatus getStatus() { return status; }

    /**
     * 要求資料更新恰好影響一筆；否則拋出 HTTP 500 的庫存一致性例外，由交易呼叫端回滾。
     */
    public static void requireOne(int rows) {
        if (rows != 1) {
            throw new BookingException("INVENTORY_CONSISTENCY_ERROR",
                    "庫存更新失敗，交易已取消", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
