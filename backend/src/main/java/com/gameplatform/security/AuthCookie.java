package com.gameplatform.security;

import org.springframework.http.ResponseCookie;

public class AuthCookie {

    public ResponseCookie create(String jwt, long expiration, boolean secure) {
        return ResponseCookie.from("ACCESS_TOKEN", jwt)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(expiration)
                .build();

    };

    public ResponseCookie clear(boolean secure) {
        return ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

}
