package com.bookx.bookx_backend.service;

import com.bookx.bookx_backend.dto.UserDto;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserDto userDto) {
        // 1) Check for existing email
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email is already registered"
            );
        }
        // 2) Check for existing username
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Username is already taken"
            );
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .passwordHash(passwordEncoder.encode(userDto.getPassword()))
                .fullName(userDto.getFullName())
                .latitude(userDto.getLatitude())
                .longitude(userDto.getLongitude())
                .profileImageUrl(userDto.getProfileImageUrl())
                .registeredAt(Instant.now())
                .build();
        return userRepository.save(user);

    }
}
