package com.example.student.security;

import com.example.student.model.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    private static UserDetails details(String email) {
        return User.withUsername(email).password("x").roles("ADMIN").build();
    }

    @Test
    @DisplayName("Токен содержит email пользователя")
    void tokenContainsUsername() {
        String token = jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("Роли из токена читаются обратно")
    void tokenContainsRoles() {
        String token = jwtService.generateToken("a@b.ru", Set.of(Role.ADMIN, Role.STUDENT));

        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder(Role.ADMIN, Role.STUDENT);
    }

    @Test
    @DisplayName("validateToken: токен подходит своему пользователю")
    void validTokenForSameUser() {
        String token = jwtService.generateToken("a@b.ru", Set.of(Role.ADMIN));

        assertThat(jwtService.validateToken(token, details("a@b.ru"))).isTrue();
    }

    @Test
    @DisplayName("validateToken: токен не подходит другому пользователю")
    void tokenIsNotValidForAnotherUser() {
        String token = jwtService.generateToken("a@b.ru", Set.of(Role.ADMIN));

        assertThat(jwtService.validateToken(token, details("other@b.ru"))).isFalse();
    }

    @Test
    @DisplayName("Просроченный токен отклоняется (ExpiredJwtException)")
    void expiredTokenIsRejected() {
        // ключ у сервиса приватный, берём его через рефлексию и выпускаем токен, который уже истёк
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtService, "SECRET_KEY");
        String expired = Jwts.builder()
                .subject("a@b.ru")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Токен, подписанный чужим ключом, отклоняется")
    void tokenSignedWithAnotherKeyIsRejected() {
        SecretKey foreignKey = Keys.hmacShaKeyFor(
                "another-secret-key-another-secret-key-another-secret-key-1234".getBytes());
        String foreign = Jwts.builder().subject("a@b.ru").signWith(foreignKey).compact();

        assertThatThrownBy(() -> jwtService.extractUsername(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Мусор вместо токена отклоняется")
    void garbageIsRejected() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-token"))
                .isInstanceOf(JwtException.class);
    }
}
