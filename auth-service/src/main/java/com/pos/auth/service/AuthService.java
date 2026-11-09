package com.pos.auth.service;

import com.pos.auth.dto.LoginRequestDto;
import com.pos.auth.dto.LoginResponseDto;
import com.pos.auth.dto.ValidateTokenResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    ValidateTokenResponseDto validateToken(String authHeader);
}
