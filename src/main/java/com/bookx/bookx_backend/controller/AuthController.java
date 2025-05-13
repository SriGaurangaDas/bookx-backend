package com.bookx.bookx_backend.controller;

import com.bookx.bookx_backend.dto.AuthRequest;
import com.bookx.bookx_backend.dto.AuthResponse;
import com.bookx.bookx_backend.dto.UserDto;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.service.AuthService;
import com.bookx.bookx_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDto userDto){
        User savedUser = userService.registerUser(userDto);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authReq){
        try{
            String token = authService.authenticateAndGetToken(authReq);
            return ResponseEntity.ok(new AuthResponse(token));
        }catch (BadCredentialsException ex){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }
}
