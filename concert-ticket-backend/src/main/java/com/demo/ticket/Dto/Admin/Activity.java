// 閱讀導引：供管理服務新增或更新活動的資料；price 是活動預設票價，供未設定分區的場次使用。
package com.demo.ticket.Dto.Admin;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class Activity {

    /**
     * -- GETTER --
     *  回傳識別碼。
     * -- SETTER --
     *  設定識別碼。

     */
    private String id;
    /**
     * -- GETTER --
     *  回傳名稱。
     * -- SETTER --
     *  設定名稱。

     */
    private String name;
    /**
     * -- GETTER --
     *  回傳活動類別。
     * -- SETTER --
     *  設定活動類別。

     */
    private String category;
    /**
     * -- GETTER --
     *  回傳場地。
     * -- SETTER --
     *  設定場地。

     */
    private String venue;
    /**
     * -- GETTER --
     *  回傳活動預設票價，供未配置分區的場次計價。
     * -- SETTER --
     *  設定活動預設票價；分區票價由場次售票設定另行管理。

     */
    private BigDecimal price;
    /**
     * -- GETTER --
     *  回傳活動介紹。
     * -- SETTER --
     *  設定活動介紹。

     */
    private String description;

}
