package com.gameplatform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class CookieBearerTokenResolver implements org.springframework.security.oauth2.server.resource.web.BearerTokenResolver {

    private final DefaultBearerTokenResolver authorizationHeaderResolver =
            new DefaultBearerTokenResolver();


    @Override
    public String resolve(HttpServletRequest request) {
        // 1) Header Authorization: Bearer ... (Postman / old Frontend)
        String fromHeader = authorizationHeaderResolver.resolve(request);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        // 2) Cookie ACCESS_TOKEN
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("ACCESS_TOKEN".equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null; // Spring → 401 nếu endpoint cần auth
    }
}
