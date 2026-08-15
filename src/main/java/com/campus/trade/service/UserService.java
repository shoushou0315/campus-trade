package com.campus.trade.service;

import com.campus.trade.dto.request.UserLoginDTO;
import com.campus.trade.dto.request.UserRegisterDTO;
import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;

public interface UserService {

    LoginVO register(UserRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    UserVO getCurrentUser(Long userId);
}
