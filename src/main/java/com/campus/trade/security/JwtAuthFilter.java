package com.campus.trade.security;

import com.campus.trade.entity.UserDO;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final TokenService tokenService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserMapper userMapper, TokenService tokenService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        // access token：验签 + 未过期 + Redis 白名单一致（登出/踢下线/换设备后自动失效）
        if (StringUtils.hasText(token) && tokenService.isAccessValid(token)) {
            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);

            UserDO user = userMapper.selectByUsername(username);
            if (user != null && user.getStatus() == 1) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
