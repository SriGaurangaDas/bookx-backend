package com.bookx.bookx_backend.service;

import com.bookx.bookx_backend.config.PasswordEncoderConfig;
import com.bookx.bookx_backend.dto.UserDto;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Import({UserService.class, PasswordEncoderConfig.class})
class UserServiceTest {

    @Autowired
    UserRepository userRepo;
    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder encoder;

    @Test
    void registerNewUser() {
        var dto = new UserDto("u","e","pass","",0.0,0.0,"");
        User saved = userService.registerUser(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepo.findByEmail("e")).isPresent();
    }

    @Test
    void duplicateEmailThrows() {
        var dto1 = new UserDto("u1","e","p","",0.0,0.0,"");
        userService.registerUser(dto1);

        var dto2 = new UserDto("u2","e","p","",0.0,0.0,"");
        assertThatThrownBy(() -> userService.registerUser(dto2))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void duplicateUsernameThrows() {
        // Given: a user is already registered with a specific username
        var dto1 = new UserDto("existingUser","email1@example.com","p","",0.0,0.0,"");
        userService.registerUser(dto1);

        // When: attempting to register another user with the same username but different email
        var dto2 = new UserDto("existingUser","email2@example.com","p","",0.0,0.0,"");
        
        // Then: a ResponseStatusException should be thrown
        assertThatThrownBy(() -> userService.registerUser(dto2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username is already taken"); // Verify the specific error message
    }

    @Test
    void passwordIsEncodedOnRegistration() {
        // Given: a user DTO with a plain text password
        String rawPassword = "securePassword123";
        var dto = new UserDto("passwordUser","password@example.com", rawPassword,"Password Test User",0.0,0.0,"");

        // When: the user is registered
        User savedUser = userService.registerUser(dto);

        // Then: the stored password hash should not be the raw password
        // And the password encoder should be able to match the raw password with the stored hash
        User retrievedUser = userRepo.findById(savedUser.getId()).orElseThrow();
        assertThat(retrievedUser.getPasswordHash()).isNotNull();
        assertThat(retrievedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, retrievedUser.getPasswordHash())).isTrue();
    }

    @Test
    void userDetailsAreCorrectlyMappedAndSaved() {
        // Given: a user DTO with all details populated
        var dto = new UserDto(
                "detailUser",
                "detail@example.com",
                "detailPass",
                "Detail Full Name",
                12.34,
                56.78,
                "http://example.com/profile.jpg"
        );

        // When: the user is registered
        User savedUser = userService.registerUser(dto);

        // Then: the retrieved user should have all details correctly mapped and 'registeredAt' set
        User retrievedUser = userRepo.findById(savedUser.getId()).orElseThrow();
        assertThat(retrievedUser.getUsername()).isEqualTo(dto.getUsername());
        assertThat(retrievedUser.getEmail()).isEqualTo(dto.getEmail());
        assertThat(retrievedUser.getFullName()).isEqualTo(dto.getFullName());
        assertThat(retrievedUser.getLatitude()).isEqualTo(dto.getLatitude());
        assertThat(retrievedUser.getLongitude()).isEqualTo(dto.getLongitude());
        assertThat(retrievedUser.getProfileImageUrl()).isEqualTo(dto.getProfileImageUrl());
        assertThat(retrievedUser.getRegisteredAt()).isNotNull();
        // Optionally, check if registeredAt is recent, e.g., within the last few seconds
        // assertThat(retrievedUser.getRegisteredAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }
}
