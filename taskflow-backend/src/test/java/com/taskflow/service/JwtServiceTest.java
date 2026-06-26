package com.taskflow.service;

import com.taskflow.entity.User;
import com.taskflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-secret-key-that-is-at-least-32-chars-long");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        User user = User.builder().email("test@test.com").role(User.Role.USER).build();
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectEmail() {
        User user = User.builder().email("test@test.com").role(User.Role.USER).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@test.com");
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        User user = User.builder().email("test@test.com")
                .passwordHash("hash").role(User.Role.USER).build();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_withWrongUser_returnsFalse() {
        User user1 = User.builder().email("a@test.com").passwordHash("h").role(User.Role.USER).build();
        User user2 = User.builder().email("b@test.com").passwordHash("h").role(User.Role.USER).build();
        String token = jwtService.generateToken(user1);
        assertThat(jwtService.isTokenValid(token, user2)).isFalse();
    }
}
