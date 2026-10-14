// 閱讀導引：供註冊 SQL 寫入的會員資料；password 應為服務層產生的 BCrypt 雜湊。
package com.demo.ticket.Dto.Login;

public class Register {

    private String account;
    private String name;
    private String email;
    private String phone;
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

    /**
     * 回傳會員帳號。
     */
    public String getAccount() {
        return account;
    }

    /**
     * 設定會員帳號。
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * 回傳名稱。
     */
    public String getName() {
        return name;
    }

    /**
     * 設定名稱。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 回傳會員電子郵件。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 設定會員電子郵件。
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 回傳電話。
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 設定電話。
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 回傳待寫入資料庫的 BCrypt 密碼雜湊，不是註冊請求中的明文密碼。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 設定已編碼的密碼雜湊；編碼處理由註冊服務呼叫 BCrypt 完成。
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
