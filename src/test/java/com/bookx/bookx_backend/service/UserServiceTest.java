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
}

