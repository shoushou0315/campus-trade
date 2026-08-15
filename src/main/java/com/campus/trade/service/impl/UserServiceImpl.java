package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.dto.request.UserLoginDTO;
import com.campus.trade.dto.request.UserRegisterDTO;
import com.campus.trade.entity.UserDO;
import com.campus.trade.enums.UserStatusEnum;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.service.TokenService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginVO register(UserRegisterDTO dto) {
        UserDO exist = userMapper.selectByUsername(dto.getUsername());
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setSchool(dto.getSchool());
        user.setRole("USER");
        user.setStatus(UserStatusEnum.ENABLED.getCode());

        userMapper.insert(user);
        logger.info("用户{}注册成功", dto.getUsername());

        return tokenService.issueTokens(user.getId(), user.getUsername(), user.getRole(), UserVO.from(user));
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        UserDO user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
            if (user.getStatus() == UserStatusEnum.DISABLED.getCode()) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        logger.info("用户{}登录成功", dto.getUsername());
        return tokenService.issueTokens(user.getId(), user.getUsername(), user.getRole(), UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.from(user);
    }
}
