package com.divination.config;
import com.divination.common.constant.JwtConstants;
import com.divination.common.util.JwtUtil;
import com.divination.entity.User;
import com.divination.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserService userService;
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(JwtConstants.HEADER);
        if (header != null && header.startsWith(JwtConstants.PREFIX)) {
            String token = header.substring(JwtConstants.PREFIX.length());
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userService.getById(userId);
                    if (user != null && jwtUtil.isTokenVersionValid(token, user.getTokenVersion())) {
                        String authority = "ROLE_" + (role != null ? role : "USER");
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userId, null,
                                        Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)));
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        request.setAttribute("userId", userId);
                        request.setAttribute("username", username);
                        request.setAttribute("role", role);
                    }
                    // user deleted or version mismatch -> silently skip auth
                }
            } catch (Exception ignored) {
                // Token invalid, continue without authentication
            }
        }
        filterChain.doFilter(request, response);
    }
}
