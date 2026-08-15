package com.campus.trade.controller;

import com.campus.trade.common.result.Result;
import com.campus.trade.entity.UserDO;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.UserVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<UserVO> getUserInfo(@AuthenticationPrincipal UserDO user) {
        UserVO vo = userService.getCurrentUser(user.getId());
        return Result.success(vo);
    }
}
