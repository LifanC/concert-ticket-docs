// 閱讀導引：簽章金鑰的資料庫介面；方法對應 resources/Mapper/SecretMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecretMapper {

    /**
     * 檢查資料庫是否已有本年、本月建立的簽章祕密，回傳存在與否。
     */
    boolean getSecret();

    /**
     * 將新產生的簽章祕密寫入資料庫。
     */
    void createSecret(String secretNum);

    /**
     * 取得本年、本月建立的祕密字串，供 JWT 簽章與驗證使用。
     */
    String getSecretOnly();
}
