package com.example.student.service;

import com.example.student.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomUserDetailsServiceTest extends IntegrationTestBase {

    @Autowired
    private CustomUserDetailsService service;

    @Test
    @DisplayName("Пользователь находится по email, роль получает приставку ROLE_")
    void loadsUserWithRoleAuthorities() {
        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertThat(details.getUsername()).isEqualTo("admin@example.com");
        assertThat(details.getPassword()).isNotBlank().isNotEqualTo("admin");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Студент получает ROLE_STUDENT")
    void studentAuthority() {
        UserDetails details = service.loadUserByUsername("student@example.com");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Неизвестный email: UsernameNotFoundException")
    void unknownUserThrows() {
        assertThatThrownBy(() -> service.loadUserByUsername("none@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
