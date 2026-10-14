// 閱讀導引：保留發生錯誤的表單欄位，讓前端能將驗證訊息顯示在對應輸入位置。
package com.demo.ticket.Exception;

public class FieldValidationException extends RuntimeException {

    private final String field;

    /**
     * 建立指定表單欄位的驗證例外；帶 cause 的版本同時保留原始失敗原因，供診斷使用。
     */
    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * 建立指定表單欄位的驗證例外；帶 cause 的版本同時保留原始失敗原因，供診斷使用。
     */
    public FieldValidationException(String field, String message, Throwable cause) {
        super(message, cause);
        this.field = field;
    }

    /**
     * 回傳發生驗證錯誤的欄位。
     */
    public String getField() {
        return field;
    }
}
