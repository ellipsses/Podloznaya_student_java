package com.example.student.service.impl;

import com.example.student.IntegrationTestBase;
import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.exception.EmailAlreadyExistsException;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.model.Role;
import com.example.student.model.User;
import com.example.student.repository.UserRepository;
import com.example.student.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Сервис работает с настоящим репозиторием и временной базой, заглушек нет. */
class UserServiceImplTest extends IntegrationTestBase {

    @Autowired
    private UserService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Готовые аккаунты admin и student есть в базе")
    void seededUsersExist() {
        assertThat(service.existsByEmail("admin@example.com")).isTrue();
        assertThat(service.existsByEmail("student@example.com")).isTrue();
        assertThat(service.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("save: пароль в базе хранится в виде хэша, а не открытым текстом")
    void savedPasswordIsHashed() {
        UserResponse saved = service.save(new UserRequest("hash@example.com", "secret1", Set.of(Role.STUDENT)));

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getEmail()).isEqualTo("hash@example.com");
        assertThat(saved.getRoles()).containsExactly(Role.STUDENT);
        User stored = userRepository.findByEmail("hash@example.com").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secret1");
        assertThat(passwordEncoder.matches("secret1", stored.getPassword())).isTrue();
    }

    @Test
    @DisplayName("save: если email уже есть, EmailAlreadyExistsException")
    void saveDuplicateEmailThrows() {
        assertThatThrownBy(() -> service.save(new UserRequest("admin@example.com", "secret1", Set.of(Role.ADMIN))))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email admin@example.com is already exists");
    }

    @Test
    @DisplayName("register: без email регистрация не проходит")
    void registerWithoutEmail() {
        assertThat(service.register(new UserRequest(null, "secret1", Set.of(Role.STUDENT)))).isFalse();
    }

    @Test
    @DisplayName("register: занятый email не принимается")
    void registerExistingEmail() {
        assertThat(service.register(new UserRequest("admin@example.com", "secret1", Set.of(Role.STUDENT)))).isFalse();
    }

    @Test
    @DisplayName("register: новый пользователь сохраняется с хэшем пароля")
    void registerNewUser() {
        boolean result = service.register(new UserRequest("reg@example.com", "secret1", Set.of(Role.STUDENT)));

        assertThat(result).isTrue();
        User stored = userRepository.findByEmail("reg@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("secret1", stored.getPassword())).isTrue();
    }

    @Test
    @DisplayName("register: пустой пароль не шифруется")
    void registerBlankPasswordIsKept() {
        boolean result = service.register(new UserRequest("blank@example.com", "  ", Set.of(Role.STUDENT)));

        assertThat(result).isTrue();
        assertThat(userRepository.findByEmail("blank@example.com").orElseThrow().getPassword()).isEqualTo("  ");
    }

    @Test
    @DisplayName("register: пароль null не шифруется, а база такого пользователя не принимает")
    void registerNullPasswordIsRejectedByDatabase() {
        assertThatThrownBy(() -> service.register(new UserRequest("nopass@example.com", null, Set.of(Role.STUDENT))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("findByEmail: находит пользователя, а если нет, EntityNotFoundException")
    void findByEmail() {
        assertThat(service.findByEmail("admin@example.com").getRoles()).containsExactly(Role.ADMIN);

        assertThatThrownBy(() -> service.findByEmail("nobody@example.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User with email nobody@example.com not found!");
    }

    @Test
    @DisplayName("findAll: возвращает всех пользователей и количество")
    void findAllReturnsUsers() {
        UserResponseList result = service.findAll();

        assertThat(result.getTotalCount()).isGreaterThanOrEqualTo(2);
        assertThat(result.getResponses()).extracting(UserResponse::getEmail)
                .contains("admin@example.com", "student@example.com");
    }

    @Test
    @DisplayName("updateUser: меняет роли пользователя, найденного по email")
    void updateUserChangesRoles() {
        service.save(new UserRequest("promote@example.com", "secret1", Set.of(Role.STUDENT)));

        service.updateUser(new UserRequest("promote@example.com", null, Set.of(Role.ADMIN)));

        assertThat(service.findByEmail("promote@example.com").getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    @DisplayName("updateUser: неизвестный email, EntityNotFoundException")
    void updateUnknownUser() {
        assertThatThrownBy(() -> service.updateUser(new UserRequest("nobody@example.com", null, Set.of(Role.ADMIN))))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById: пользователь удаляется")
    void deleteById() {
        long id = service.save(new UserRequest("temp@example.com", "secret1", Set.of(Role.STUDENT))).getId();

        service.deleteById(id);

        assertThat(service.existsByEmail("temp@example.com")).isFalse();
    }
}
