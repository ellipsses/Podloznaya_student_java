package com.example.student.mapper;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.model.Role;
import com.example.student.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperImplTest {

    private final UserMapperImpl mapper = new UserMapperImpl();

    private static User user(long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword("HASH");
        u.setRoles(Set.of(role));
        return u;
    }

    @Test
    @DisplayName("В ответ пользователя пароль не попадает: только ID, email и роли")
    void toResponse() {
        UserResponse r = mapper.toResponse(user(1, "admin@example.com", Role.ADMIN));

        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getEmail()).isEqualTo("admin@example.com");
        assertThat(r.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    @DisplayName("Запрос превращается в сущность User")
    void toEntity() {
        User u = mapper.toEntity(new UserRequest("a@b.ru", "secret", Set.of(Role.STUDENT)));

        assertThat(u.getId()).isNull();
        assertThat(u.getEmail()).isEqualTo("a@b.ru");
        assertThat(u.getPassword()).isEqualTo("secret");
        assertThat(u.getRoles()).containsExactly(Role.STUDENT);
    }

    @Test
    @DisplayName("Список пользователей превращается в ответ со счётчиком")
    void toListResponse() {
        UserResponseList r = mapper.toListResponse(List.of(
                user(1, "admin@example.com", Role.ADMIN),
                user(2, "student@example.com", Role.STUDENT)));

        assertThat(r.getTotalCount()).isEqualTo(2);
        assertThat(r.getResponses()).extracting(UserResponse::getEmail)
                .containsExactly("admin@example.com", "student@example.com");
    }
}
