package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Login.Login;
import com.demo.ticket.Dto.Login.LoginSaveProfile;
import com.demo.ticket.Dto.Login.Register;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginMapper {

    void create(Register register);

    Map<String, Object> select(Login login);

    void save(LoginSaveProfile loginSaveProfile);

    List<String> selectPermissions(String account);

}
