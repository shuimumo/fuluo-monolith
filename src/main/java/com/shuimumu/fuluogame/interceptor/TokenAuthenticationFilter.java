package com.shuimumu.fuluogame.interceptor;

import cn.hutool.json.JSONUtil;
import com.shuimumu.fuluogame.constants.SystemRedisKeys;
import com.shuimumu.fuluogame.vo.UserVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    public TokenAuthenticationFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
       if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String userJson = stringRedisTemplate.opsForValue().get(SystemRedisKeys.loginToken(token));
            if (userJson != null) {
                UserVO user = JSONUtil.toBean(userJson, UserVO.class);
                List<SimpleGrantedAuthority> authorities = "admin".equals(user.getRole())
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.emptyList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
