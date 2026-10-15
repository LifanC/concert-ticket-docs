// 閱讀導引：供註冊 SQL 寫入的會員資料；password 應為服務層產生的 BCrypt 雜湊。
package com.demo.ticket.Dto.Login;

import lombok.*;

@Setter
@Getter
@Builder
public class Register {

    /**
     * -- GETTER --
     *  回傳會員帳號。
     * -- SETTER --
     *  設定會員帳號。

     */
    private String account;
    /**
     * -- GETTER --
     *  回傳名稱。
     * -- SETTER --
     *  設定名稱。

     */
    private String name;
    /**
     * -- GETTER --
     *  回傳會員電子郵件。
     * -- SETTER --
     *  設定會員電子郵件。

     */
    private String email;
    /**
     * -- GETTER --
     *  回傳電話。
     * -- SETTER --
     *  設定電話。

     */
    private String phone;
    /**
     * -- GETTER --
     *  回傳待寫入資料庫的 BCrypt 密碼雜湊，不是註冊請求中的明文密碼。
     * -- SETTER --
     *  設定已編碼的密碼雜湊；編碼處理由註冊服務呼叫 BCrypt 完成。

     */
    private String password;

    /**
     * 建立註冊資料；有參數版本可一次填入會員欄位，password 應已由服務層編碼為 BCrypt 雜湊。
     */
    public Register() {
    }

    /**
     * 建立註冊資料；有參數版本可一次填入會員欄位，password 應已由服務層編碼為 BCrypt 雜湊。
     */
    public Register(String account, String name, String email, String phone, String password) {
        this.account = account;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

}
