package com.divination.config;

import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 安全响应头过滤器 - 为所有 HTTP 响应添加安全相关的头部信息
 */
@Component
public class XHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 为每个请求生成唯一 ID，方便追踪日志
        String requestId = UUID.randomUUID().toString().replace("-", "");
        httpResponse.setHeader("X-Request-ID", requestId);
        httpResponse.setHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));

        // 安全头部防护
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");          // 防止 MIME 嗅探攻击
        httpResponse.setHeader("X-Frame-Options", "DENY");                    // 防止点击劫持（与 security header 冗余但双重保险）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");          // XSS 防护
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin"); // Referrer 策略

        // CSP (Content Security Policy) - 根据实际前端部署调整
        // 仅允许从自身域名加载资源，防止脚本注入
        httpResponse.setHeader(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self';"
        );

        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化逻辑（如有需要）
    }

    @Override
    public void destroy() {
        // 销毁逻辑（如有需要）
    }
}
