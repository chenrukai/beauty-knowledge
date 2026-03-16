package com.beauty.knowledge.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beauty.knowledge.module.auth.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
