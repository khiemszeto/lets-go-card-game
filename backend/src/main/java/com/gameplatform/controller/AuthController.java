package com.gameplatform.controller;

import com.gameplatform.dto.LoginRequestDto;
import com.gameplatform.dto.LoginResponseDto;
import com.gameplatform.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {



    private AuthenticationManager authenticationManager;

    private JwtService jwtService;

    private final ResourceLoader resourceLoader;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          ResourceLoader resourceLoader) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.resourceLoader = resourceLoader;
    }


    @PostMapping("/login")
    public LoginResponseDto login(
            @RequestBody LoginRequestDto loginRequestDto) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                );

        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        String token = jwtService.generateToken(authentication);

        return new LoginResponseDto(token);
    }
}