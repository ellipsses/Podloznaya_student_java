package com.example.student.controller;

import com.example.student.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends IntegrationTestBase {

    private static final String BASE = "/api/v1/users";

    /** Создаёт пользователя от имени администратора и возвращает его ID. */
    private long createUser(String email, String password, String roleName) throws Exception {
        String body = mvc.perform(post(BASE + "/save")
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\","
                                + "\"roles\":[\"" + roleName + "\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String listAsAdmin() throws Exception {
        return mvc.perform(get(BASE).header("Authorization", adminHeader()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<String> rolesOf(String listJson, String email) {
        return JsonPath.read(listJson, "$.responses[?(@.email=='" + email + "')].roles[*]");
    }

    @Test
    @DisplayName("GET: администратор видит готовые аккаунты, пароли в ответ не попадают")
    void adminSeesUsersWithoutPasswords() throws Exception {
        mvc.perform(get(BASE).header("Authorization", adminHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses[?(@.email=='admin@example.com')]").exists())
                .andExpect(jsonPath("$.responses[?(@.email=='student@example.com')]").exists())
                .andExpect(jsonPath("$.responses[0].password").doesNotExist());
    }

    @Test
    @DisplayName("GET: студенту список пользователей недоступен (403)")
    void studentCannotListUsers() throws Exception {
        mvc.perform(get(BASE).header("Authorization", studentHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET: без токена недоступно (403)")
    void anonymousCannotListUsers() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST save: созданный пользователь может войти и работает с правами студента")
    void createdUserCanLogInWithOwnRole() throws Exception {
        createUser("new.student@example.com", "secret1", "STUDENT");

        String token = login("new.student@example.com", "secret1");

        mvc.perform(get("/api/v1/student").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/student/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST save: студент пользователей создавать не может (403)")
    void studentCannotCreateUsers() throws Exception {
        mvc.perform(post(BASE + "/save")
                        .header("Authorization", studentHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@y.ru\",\"password\":\"secret1\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT: администратор меняет роли пользователя")
    void adminChangesRoles() throws Exception {
        long id = createUser("promote@example.com", "secret1", "STUDENT");
        assertThat(rolesOf(listAsAdmin(), "promote@example.com")).containsExactly("STUDENT");

        mvc.perform(put(BASE + "/" + id)
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"promote@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk());

        assertThat(rolesOf(listAsAdmin(), "promote@example.com")).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("PUT: студент роли менять не может (403)")
    void studentCannotChangeRoles() throws Exception {
        mvc.perform(put(BASE + "/2")
                        .header("Authorization", studentHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE: администратор удаляет пользователя, из списка он пропадает")
    void adminDeletesUser() throws Exception {
        long id = createUser("temp@example.com", "secret1", "STUDENT");

        mvc.perform(delete(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(status().isOk());

        assertThat(rolesOf(listAsAdmin(), "temp@example.com")).isEmpty();
    }

    @Test
    @DisplayName("DELETE: студент пользователей удалять не может (403)")
    void studentCannotDeleteUsers() throws Exception {
        mvc.perform(delete(BASE + "/1").header("Authorization", studentHeader()))
                .andExpect(status().isForbidden());
    }
}
