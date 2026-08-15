package com.campus.trade.service;

import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;

public interface TokenService {

    /** 登录/注册成功后颁发双 token，写入 Redis 白名单（单设备登录） */
    LoginVO issueTokens(Long userId, String username, String role, UserVO userVO);

    /** 校验 access token 是否仍有效（与 Redis 白名单比对） */
    boolean isAccessValid(String accessToken);

    /** 用 refresh token 换发新 access+refresh（轮换 refresh，旧 refresh 失效） */
    LoginVO refresh(String refreshToken, UserVO userVO);

    /** 登出：删除 Redis 中该用户全部 token */
    void logout(Long userId);

    /** 吊销某用户全部 token（改密/封号/踢下线） */
    void revoke(Long userId);
}
