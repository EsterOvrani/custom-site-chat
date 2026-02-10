package com.example.backend.user.controller;

import com.example.backend.user.model.User;
import com.example.backend.user.service.UserService;
import com.example.backend.user.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/users") 
@RestController
public class UserController {
    
    private final UserService userService;
    
    private final TokenService tokenService;

    public UserController(UserService userService, com.example.backend.user.service.TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    // Get current user details
    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    // Get token usage info
    @GetMapping("/me/tokens")
    public ResponseEntity<com.example.backend.user.service.TokenService.TokenUsageInfo> getTokenUsage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        com.example.backend.user.service.TokenService.TokenUsageInfo tokenInfo = 
            tokenService.getTokenUsage(currentUser);
        
        return ResponseEntity.ok(tokenInfo);
    }

    // Get all users (admin)
    @GetMapping("/")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }
}