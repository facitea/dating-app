/**
 * JwtFilter 설정
 */
package com.dating.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (!request.getRequestURI().startsWith("/api/admin/") && StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmail(token);
                String path=request.getRequestURI();
                boolean allowed=path.equals("/api/user/me")||path.startsWith("/api/selfie")||path.startsWith("/api/auth/")||path.startsWith("/api/support/")||path.equals("/api/profile/me")||path.startsWith("/api/profile-images/")||path.startsWith("/api/account/")||path.startsWith("/api/media/");
                if(jwtUtil.selfieRequired(email)&&!allowed){response.setStatus(409);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"code\":\"SELFIE_REQUIRED\",\"message\":\"운영자가 요청한 셀피 확인을 완료해 주세요.\"}");return;}
                request.setAttribute("email", email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}