// 閱讀導引：WebSocket 通知的資料結構，包含接收會員與畫面要顯示的訊息。
package com.demo.ticket.Config.WebSocket;

import lombok.*;

@Setter
@Getter
public class NotificationMessage {

    /**
     * -- GETTER --
     *  回傳會員電子郵件。
     * -- SETTER --
     *  設定會員電子郵件。

     */
    private String email;
    /**
     * -- GETTER --
     *  回傳通知標題。
     * -- SETTER --
     *  設定通知標題。

     */
    private String title;
    /**
     * -- GETTER --
     *  回傳通知內容。
     * -- SETTER --
     *  設定通知內容。

     */
    private String content;
    /**
     * -- GETTER --
     *  取得通知去重識別碼；未設定時前端不對此通知去重。
     * -- SETTER --
     *  設定通知識別碼，讓同一事件從 API 與 WebSocket 到達時只顯示一次。

     */
    private String id;
    /**
     * -- GETTER --
     *  取得前端提示類型，預設為 success。
     * -- SETTER --
     *  設定前端提示類型，例如 success、warning、error 或 info。

     */
    private String type = "success";

    /**
     * 建立指定會員的標題及內容通知；類型預設為 success，id 由呼叫端視需要設定以支援前端去重。
     */
    @Builder
    public NotificationMessage(String email, String title, String content) {
        this.email = email;
        this.title = title;
        this.content = content;
    }

}
