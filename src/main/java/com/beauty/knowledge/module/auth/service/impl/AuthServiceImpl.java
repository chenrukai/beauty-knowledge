package com.beauty.knowledge.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.common.constant.RedisKeyConstant;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import com.beauty.knowledge.common.util.JwtUtil;
import com.beauty.knowledge.common.util.SecurityUtil;
import com.beauty.knowledge.module.auth.domain.dto.LoginRequest;
import com.beauty.knowledge.module.auth.domain.entity.SysUser;
import com.beauty.knowledge.module.auth.domain.vo.LoginResponse;
import com.beauty.knowledge.module.auth.domain.vo.UserInfoVO;
import com.beauty.knowledge.module.auth.mapper.SysUserMapper;
import com.beauty.knowledge.module.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_FAIL_MSG = "用户名或密码错误";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${beauty.jwt.expire-in-seconds:86400}")
    private Long expireInSeconds;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           StringRedisTemplate stringRedisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser == null || !passwordEncoder.matches(request.getPassword(), sysUser.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, LOGIN_FAIL_MSG);
        }
        if (!Integer.valueOf(1).equals(sysUser.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用");
        }

        String token = jwtUtil.generateToken(sysUser.getId(), sysUser.getRole());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expireIn(expireInSeconds)
                .userInfo(toUserInfo(sysUser))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (!jwtUtil.validate(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token无效");
        }
        Long userId = jwtUtil.getUserId(token);
        long remaining = jwtUtil.getRemainingExpire(token);
        long ttlSeconds = Math.max(remaining, 1L);
        String key = RedisKeyConstant.userToken(userId);
        stringRedisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS);
        log.info("User logout and token blacklisted, userId={}", userId);
    }

    @Override
    public UserInfoVO getInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toUserInfo(sysUser);
    }

    private UserInfoVO toUserInfo(SysUser sysUser) {
        return UserInfoVO.builder()
                .id(sysUser.getId())
                .username(sysUser.getUsername())
                .nickname(sysUser.getNickname())
                .role(sysUser.getRole())
                .build();
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少有效Token");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
