package com.maaz.saasPlatform.auth.controller;

import com.maaz.saasPlatform.auth.util.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        var principal = auth.getName();
        var role = auth.getAuthorities().iterator().next().getAuthority();

        return JwtUtil.generateToken(principal, role);
    }
}
