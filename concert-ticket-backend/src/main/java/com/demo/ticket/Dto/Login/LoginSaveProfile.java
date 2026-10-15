// 閱讀導引：供會員資料 SQL 比對姓名與 email，並更新電話及生日；此 SQL 不寫入 permissions。
package com.demo.ticket.Dto.Login;

import lombok.*;

@Setter
@Getter
@Builder
public class LoginSaveProfile {

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
     *  回傳生日。
     * -- SETTER --
     *  設定生日。

     */
    private String birthday;
    /**
     * -- GETTER --
     *  回傳會員權限清單。
     * -- SETTER --
     *  設定會員權限清單。

     */
    private String permissions;

}
