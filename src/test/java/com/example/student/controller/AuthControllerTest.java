package com.example.student.controller;

import com.example.student.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/v1/auth/login";

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    /** Неверный вход сервер отклоняет: ответ 401/403 или ошибка аутентификации внутри Spring. */
    private void assertLoginRejected(String email, String password) throws Exception {
        int status;
        try {
            status = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, password))).andReturn().getResponse().getStatus();
        } catch (Exception e) {
            assertThat(e).hasRootCauseInstanceOf(AuthenticationException.class);
            return;
        }
        assertThat(status).isIn(401, 403);
    }

    @Test
    @DisplayName("Вход admin: токен Bearer на 3600 секунд и роль ROLE_ADMIN")
    void adminLogin() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("admin@example.com", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Вход student: роль ROLE_STUDENT")
    void studentLogin() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("student@example.com", "student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("Выданный токен открывает защищённые ручки")
    void issuedTokenWorks() throws Exception {
        mvc.perform(get("/api/v1/student").header("Authorization", studentHeader()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Неверный пароль отклоняется")
    void wrongPasswordIsRejected() throws Exception {
        assertLoginRejected("admin@example.com", "wrong-password");
    }

    @Test
    @DisplayName("Неизвестный email отклоняется")
    void unknownUserIsRejected() throws Exception {
        assertLoginRejected("nobody@example.com", "whatever");
    }

    @Test
    @DisplayName("Некорректный email: 400")
    void invalidEmailGives400() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", "admin")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Пустой пароль: 400")
    void blankPasswordGives400() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("admin@example.com", "")))
                .andExpect(status().isBadRequest());
    }
}
