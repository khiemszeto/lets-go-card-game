package com.gameplatform.controller;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.dto.LoginRequestDto;
import com.gameplatform.dto.LoginResponseDto;
import com.gameplatform.entity.CustomUserDetails;
import com.gameplatform.service.AuthPlayerService;
import com.gameplatform.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AuthController {


    private final AuthPlayerService authPlayerService;
    private AuthenticationManager authenticationManager;

    private JwtService jwtService;

    private final ResourceLoader resourceLoader;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          ResourceLoader resourceLoader, AuthPlayerService authPlayerService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.resourceLoader = resourceLoader;
        this.authPlayerService = authPlayerService;
    }


    @PostMapping("/login")
    public LoginResponseDto login(
            @RequestBody LoginRequestDto loginRequestDto) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequestDto.getUsername(),
                                                                    loginRequestDto.getPassword());

        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        String token = jwtService.generateToken(authentication);

        CustomUserDetails player = (CustomUserDetails) authentication.getPrincipal();

        return new LoginResponseDto(token, player.getBalance());
    }
}