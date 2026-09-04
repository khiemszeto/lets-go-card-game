package com.gameplatform.controller;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.dto.LoginRequestDto;
import com.gameplatform.dto.LoginResponseDto;
import com.gameplatform.entity.CustomUserDetails;
import com.gameplatform.security.AuthCookie;
import com.gameplatform.service.AuthPlayerService;
import com.gameplatform.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class AuthController {


    private AuthPlayerService authPlayerService;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private final Long jwtExpiration;
    private final boolean cookieSecure;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          AuthPlayerService authPlayerService,
                          @Value("${jwt.expiration}") Long jwtExpiration,
                          @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtExpiration = jwtExpiration;
        this.cookieSecure = cookieSecure;
        this.authPlayerService = authPlayerService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto loginRequestDto) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequestDto.getUsername(),
                                                                    loginRequestDto.getPassword());

        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        String token = jwtService.generateToken(authentication);

        ResponseCookie cookie = new AuthCookie().create(token, this.jwtExpiration, this.cookieSecure);

        CustomUserDetails player = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponseDto(player.getUsername(), player.getBalance()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = new AuthCookie().clear(cookieSecure);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/api/me")
    public LoginResponseDto getMe(@AuthenticationPrincipal Jwt jwt) {
        Number id = jwt.getClaim("playerId");


        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing playerId");
        }

        Long playerId = id.longValue();

        CreatePlayerResponseDto player = authPlayerService.getPlayer(playerId);
        return new LoginResponseDto(player.getUsername(), player.getBalance());
    }

}