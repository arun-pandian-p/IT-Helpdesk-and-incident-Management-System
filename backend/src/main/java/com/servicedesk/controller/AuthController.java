package com.servicedesk.controller;

import com.servicedesk.dto.AuthDtos.LoginRequest;
import com.servicedesk.dto.AuthDtos.RegisterRequest;
import com.servicedesk.dto.AuthDtos.TokenResponse;
import com.servicedesk.dto.AuthDtos.UserDto;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.SeedService;
import com.servicedesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final SeedService seedService;

    public AuthController(UserService userService, SeedService seedService) {
        this.userService = userService;
        this.seedService = seedService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        TokenResponse res = userService.login(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        TokenResponse res = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = userService.getMe(principal);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedDemoData() {
        seedService.seedDatabase();
        return ResponseEntity.ok(Map.of(
                "message", "Demo data populated successfully with 16 users and 105+ tickets."
        ));
    }
}
