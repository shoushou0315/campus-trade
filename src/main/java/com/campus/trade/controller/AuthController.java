package com.campus.trade.controller;

import com.campus.trade.common.annotation.RateLimit;
import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.Result;
import com.campus.trade.dto.request.UserLoginDTO;
import com.campus.trade.dto.request.UserRegisterDTO;
import com.campus.trade.entity.UserDO;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.service.TokenService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, TokenService tokenService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    @RateLimit(maxRequests = 5)
    public Result<LoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    @RateLimit(maxRequests = 10)
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** 刷新：用 refresh token 换发新 access+refresh（需登录态传 refreshToken） */
    @PostMapping("/refresh")
    @RateLimit(maxRequests = 30)
    public Result<LoginVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("refreshToken 不能为空");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        UserVO userVO = userService.getCurrentUser(userId);
        return Result.success(tokenService.refresh(refreshToken, userVO));
    }

    /** 登出：吊销该用户全部 token */
    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal UserDO user) {
        tokenService.logout(user.getId());
        return Result.success(null);
    }
}
