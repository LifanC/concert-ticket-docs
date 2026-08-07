package com.demo.ticket.Service;

import com.demo.ticket.Dto.Login.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface LoginService {

    ResponseEntity<?> register(@Valid RegisterRequest request);

    ResponseEntity<?> login(@Valid LoginRequest request);

    ResponseEntity<?> validate(@Valid LoginTokenValidateRequest request);

    ResponseEntity<?> saveProfile(@Valid LoginSaveProfileRequest request);

    ResponseEntity<?> logout(@Valid LoginLogoutRequest request);
}
