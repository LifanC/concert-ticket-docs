package com.demo.ticket.Service;

import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface LoginService {

    ResponseEntity<?> register(@Valid RegisterRequest request);

    ResponseEntity<?> login(@Valid LoginRequest request);

    ResponseEntity<?> validate(String refreshToken);

    ResponseEntity<?> saveProfile(@Valid LoginSaveProfileRequest request, LoginUser user);

    ResponseEntity<?> logout(LoginUser user, String refreshToken);
}
