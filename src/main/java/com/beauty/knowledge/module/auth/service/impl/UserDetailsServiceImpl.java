package com.beauty.knowledge.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.module.auth.domain.entity.SysUser;
import com.beauty.knowledge.module.auth.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (Integer.valueOf(0).equals(sysUser.getStatus())) {
            throw new DisabledException("账号已禁用");
        }

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + sysUser.getRole().toUpperCase());
        return User.withUsername(String.valueOf(sysUser.getId()))
                .password(sysUser.getPassword())
                .authorities(List.of(authority))
                .disabled(false)
                .build();
    }
}
