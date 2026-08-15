package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.service.TokenService;
import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final String ACCESS_KEY = "token:access:";
    private static final String REFRESH_KEY = "token:refresh:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    public TokenServiceImpl(JwtUtil jwtUtil, StringRedisTemplate redis) {
        this.jwtUtil = jwtUtil;
        this.redis = redis;
    }

    @Override
    public LoginVO issueTokens(Long userId, String username, String role, UserVO userVO) {
        String access = jwtUtil.generateAccessToken(userId, username, role);
        String refresh = jwtUtil.generateRefreshToken(userId, username, role);

        // 覆盖写 = 单设备登录（旧设备 access 立即失效）
        redis.opsForValue().set(ACCESS_KEY + userId, access, 15, TimeUnit.MINUTES);
        redis.opsForValue().set(REFRESH_KEY + userId, refresh, 7, TimeUnit.DAYS);

        logger.info("用户{}颁发双token(单设备)", username);
        return new LoginVO(access, refresh, userVO);
    }

    @Override
    public boolean isAccessValid(String accessToken) {
        try {
            if (!jwtUtil.isType(accessToken, JwtUtil.TYPE_ACCESS) || jwtUtil.isTokenExpired(accessToken)) {
                return false;
            }
            Long userId = jwtUtil.getUserId(accessToken);
            String stored = redis.opsForValue().get(ACCESS_KEY + userId);
            // 服务端比对：与 Redis 白名单一致才有效（登出/踢下线/换设备后自动失效）
            return accessToken.equals(stored);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public LoginVO refresh(String refreshToken, UserVO userVO) {
        try {
            if (!jwtUtil.isType(refreshToken, JwtUtil.TYPE_REFRESH) || jwtUtil.isTokenExpired(refreshToken)) {
                throw new BusinessException("刷新令牌无效或已过期");
            }
            Long userId = jwtUtil.getUserId(refreshToken);
            String stored = redis.opsForValue().get(REFRESH_KEY + userId);
            if (stored == null || !stored.equals(refreshToken)) {
                throw new BusinessException("刷新令牌已失效，请重新登录");
            }

            String username = jwtUtil.getUsername(refreshToken);
            String role = jwtUtil.getRole(refreshToken);

            // 轮换：新 access + 新 refresh（旧 refresh 立即失效）
            String newAccess = jwtUtil.generateAccessToken(userId, username, role);
            String newRefresh = jwtUtil.generateRefreshToken(userId, username, role);
            redis.opsForValue().set(ACCESS_KEY + userId, newAccess, 15, TimeUnit.MINUTES);
            redis.opsForValue().set(REFRESH_KEY + userId, newRefresh, 7, TimeUnit.DAYS);

            logger.info("用户{}刷新token成功", username);
            return new LoginVO(newAccess, newRefresh, userVO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
    }

    @Override
    public void logout(Long userId) {
        redis.delete(ACCESS_KEY + userId);
        redis.delete(REFRESH_KEY + userId);
        logger.info("用户{}登出，token已吊销", userId);
    }

    @Override
    public void revoke(Long userId) {
        logout(userId);
    }
}
