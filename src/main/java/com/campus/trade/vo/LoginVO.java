package com.campus.trade.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private UserVO user;

    public LoginVO() {}

    @JsonCreator
    public LoginVO(@JsonProperty("accessToken") String accessToken,
                   @JsonProperty("refreshToken") String refreshToken,
                   @JsonProperty("user") UserVO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserVO getUser() { return user; }
}
