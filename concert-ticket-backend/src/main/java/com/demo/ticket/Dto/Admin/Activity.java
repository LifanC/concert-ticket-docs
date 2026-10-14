// 閱讀導引：供管理服務新增或更新活動的資料；price 是活動預設票價，供未設定分區的場次使用。
package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

public class Activity {

    private String id;
    private String name;
    private String category;
    private String venue;
    private BigDecimal price;
    private String description;

    /**
     * 回傳識別碼。
     */
    public String getId() {
        return id;
    }

    /**
     * 設定識別碼。
     */
    public void setId(String id) {
        this.id = id;
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
     * 回傳活動類別。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 設定活動類別。
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 回傳場地。
     */
    public String getVenue() {
        return venue;
    }

    /**
     * 設定場地。
     */
    public void setVenue(String venue) {
        this.venue = venue;
    }

    /**
     * 回傳活動預設票價，供未配置分區的場次計價。
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 設定活動預設票價；分區票價由場次售票設定另行管理。
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 回傳活動介紹。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 設定活動介紹。
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
