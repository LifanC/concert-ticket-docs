// 閱讀導引：會員 email 與活動編號的收藏關聯，供收藏 SQL 新增、刪除及查詢使用。
package com.demo.ticket.Dto.Activity;

import lombok.*;

@Setter
@Getter
@Builder
public class ActivityFavorite {

    /**
     * -- GETTER --
     *  回傳會員電子郵件。
     * -- SETTER --
     *  設定會員電子郵件。

     */
    private String email;
    /**
     * -- GETTER --
     *  回傳活動編號。
     * -- SETTER --
     *  設定活動編號。

     */
    private String activity_id;

}
