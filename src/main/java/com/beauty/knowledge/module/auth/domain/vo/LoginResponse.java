package com.beauty.knowledge.module.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long expireIn;
    private UserInfoVO userInfo;
}
