package com.beauty.knowledge.module.auth.service;

import com.beauty.knowledge.module.auth.domain.dto.LoginRequest;
import com.beauty.knowledge.module.auth.domain.vo.LoginResponse;
import com.beauty.knowledge.module.auth.domain.vo.UserInfoVO;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String authorization);

    UserInfoVO getInfo();
}
