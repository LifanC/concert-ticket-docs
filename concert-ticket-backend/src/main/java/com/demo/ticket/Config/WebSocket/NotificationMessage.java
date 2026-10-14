// 閱讀導引：WebSocket 通知的資料結構，包含接收會員與畫面要顯示的訊息。
package com.demo.ticket.Config.WebSocket;

public class NotificationMessage {

    private String email;
    private String title;
    private String content;
    private String id;
    private String type = "success";

    /**
     * 取得通知去重識別碼；未設定時前端不對此通知去重。
     */
    public String getId() { return id; }
    /**
     * 設定通知識別碼，讓同一事件從 API 與 WebSocket 到達時只顯示一次。
     */
    public void setId(String id) { this.id = id; }
    /**
     * 取得前端提示類型，預設為 success。
     */
    public String getType() { return type; }
    /**
     * 設定前端提示類型，例如 success、warning、error 或 info。
     */
    public void setType(String type) { this.type = type; }

    /**
     * 建立指定會員的標題及內容通知；類型預設為 success，id 由呼叫端視需要設定以支援前端去重。
     */
    public NotificationMessage(String email, String title, String content) {
        this.email = email;
        this.title = title;
        this.content = content;
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
     * 回傳通知標題。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 設定通知標題。
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 回傳通知內容。
     */
    public String getContent() {
        return content;
    }

    /**
     * 設定通知內容。
     */
    public void setContent(String content) {
        this.content = content;
    }
}
