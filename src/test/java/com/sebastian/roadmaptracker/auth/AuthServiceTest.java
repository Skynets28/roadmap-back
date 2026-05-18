package com.sebastian.roadmaptracker.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sebastian.roadmaptracker.config.JwtProperties;
import com.sebastian.roadmaptracker.user.AppUser;
import com.sebastian.roadmaptracker.user.AppUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties props = new JwtProperties();
        props.setSecret("test_secret_must_be_at_least_32_characters_long_for_hmac");
        props.setExpirationSeconds(3600);
        jwtService = new JwtService(props);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, props);
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        String email = "user@example.com";
        String rawPassword = "secret123";
        String hash = passwordEncoder.encode(rawPassword);
        AppUser user = AppUser.builder()
            .id(UUID.randomUUID())
            .email(email)
            .passwordHash(hash)
            .enabled(true)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest(email, rawPassword));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo(email);
    }

    @Test
    void login_throwsForNonExistentEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "pass")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsForWrongPassword() {
        String email = "user@example.com";
        String hash = passwordEncoder.encode("correct");
        AppUser user = AppUser.builder()
            .id(UUID.randomUUID())
            .email(email)
            .passwordHash(hash)
            .enabled(true)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "wrong")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsForDisabledUser() {
        String email = "disabled@example.com";
        AppUser user = AppUser.builder()
            .id(UUID.randomUUID())
            .email(email)
            .passwordHash(passwordEncoder.encode("pass"))
            .enabled(false)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "pass")))
            .isInstanceOf(BadCredentialsException.class);
    }
}
