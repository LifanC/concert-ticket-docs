// 閱讀導引：會員 email 與活動編號的收藏關聯，供收藏 SQL 新增、刪除及查詢使用。
package com.demo.ticket.Dto.Activity;

public class ActivityFavorite {

    private String email;
    private String activity_id;

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
     * 回傳活動編號。
     */
    public String getActivity_id() {
        return activity_id;
    }

    /**
     * 設定活動編號。
     */
    public void setActivity_id(String activity_id) {
        this.activity_id = activity_id;
    }

}
