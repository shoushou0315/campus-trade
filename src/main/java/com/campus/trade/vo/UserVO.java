package com.campus.trade.vo;

import com.campus.trade.entity.UserDO;

public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String school;
    private String role;
    private Integer status;

    public UserVO() {}

    public static UserVO from(UserDO user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.avatar = user.getAvatar();
        vo.phone = user.getPhone();
        vo.email = user.getEmail();
        vo.school = user.getSchool();
        vo.role = user.getRole();
        vo.status = user.getStatus();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getSchool() {
        return school;
    }

    public String getRole() {
        return role;
    }

    public Integer getStatus() {
        return status;
    }
}
