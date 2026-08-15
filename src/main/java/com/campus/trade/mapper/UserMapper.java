package com.campus.trade.mapper;

import com.campus.trade.entity.UserDO;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    UserDO selectById(@Param("id") Long id);

    UserDO selectByUsername(@Param("username") String username);

    int insert(UserDO user);
}
