// 閱讀導引：供會員資料 SQL 比對姓名與 email，並更新電話及生日；此 SQL 不寫入 permissions。
package com.demo.ticket.Dto.Login;

public class LoginSaveProfile {

    private String name;
    private String email;
    private String phone;
    private String birthday;
    private String permissions;

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
     * 回傳生日。
     */
    public String getBirthday() {
        return birthday;
    }

    /**
     * 設定生日。
     */
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    /**
     * 回傳會員權限清單。
     */
    public String getPermissions() {
        return permissions;
    }

    /**
     * 設定會員權限清單。
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
