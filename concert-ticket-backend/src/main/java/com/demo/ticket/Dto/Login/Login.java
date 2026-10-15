// 閱讀導引：帳號或 email 的查詢條件；LoginMapper.select 會用同一值比對兩個欄位。
package com.demo.ticket.Dto.Login;

import lombok.*;

@Setter
@Getter
@Builder
public class Login {

    /**
     * -- GETTER --
     *  回傳用於查詢的帳號或完整 email。
     * -- SETTER --
     *  設定會員查詢值，可使用帳號或完整 email。

     */
    private String account;

    /**
     * 建立會員查詢條件；有 account 參數時可傳帳號或完整 email，無參數時供欄位映射或後續設定使用。
     */
    public Login() {
    }

    /**
     * 建立會員查詢條件；有 account 參數時可傳帳號或完整 email，無參數時供欄位映射或後續設定使用。
     */
    public Login(String account) {
        this.account = account;
    }

}
