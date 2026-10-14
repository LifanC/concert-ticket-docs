// 閱讀導引：保存處理後的圖片資料與尺寸，供活動圖片寫入資料庫。
package com.demo.ticket.Dto.Admin;

public record PreparedImage(
        int width,
        int height,
        byte[] data
) {}
