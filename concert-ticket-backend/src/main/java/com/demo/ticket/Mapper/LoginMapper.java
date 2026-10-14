// 閱讀導引：會員登入及資料的資料庫介面；方法對應 resources/Mapper/LoginMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Login.Login;
import com.demo.ticket.Dto.Login.LoginSaveProfile;
import com.demo.ticket.Dto.Login.Register;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginMapper {

    /**
     * 將註冊資料寫入會員資料表。
     */
    int create(Register register);

    /**
     * 以傳入 account 同時比對會員帳號或完整 email，回傳含密碼雜湊的會員資料，僅供後端處理。
     */
    Map<String, Object> select(Login login);

    /**
     * 以 name 與 email 作為比對條件，只更新 phone 與 birthday；此 SQL 不修改姓名，Java 方法也未回傳更新筆數。
     */
    void save(LoginSaveProfile loginSaveProfile);

    /**
     * 依會員 email 的 @ 前段比對權限資料，回傳權限代碼；目前 SQL 不是完整 email 比對。
     */
    List<String> selectPermissions(String account);

}
