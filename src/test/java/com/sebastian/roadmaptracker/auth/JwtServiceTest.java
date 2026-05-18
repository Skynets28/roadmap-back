package com.sebastian.roadmaptracker.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sebastian.roadmaptracker.config.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test_secret_must_be_at_least_32_characters_long_for_hmac");
        props.setExpirationSeconds(3600);
        jwtService = new JwtService(props);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        assertThat(jwtService.isTokenValid(token, email)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForWrongEmail() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here", "user@example.com")).isFalse();
    }
}
