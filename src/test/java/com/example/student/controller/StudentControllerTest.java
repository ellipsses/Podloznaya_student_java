package com.example.student.controller;

import com.example.student.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerTest extends IntegrationTestBase {

    private static final String BASE = "/api/v1/student";

    /** Добавляет студента от имени администратора и возвращает его ID. */
    private long createStudent(String name, String surname, String phone, String email) throws Exception {
        String body = mvc.perform(post(BASE + "/save")
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"surname\":\"" + surname + "\","
                                + "\"phone_number\":\"" + phone + "\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // GET /api/v1/student

    @Test
    @DisplayName("GET список: администратор видит студентов (в базе уже есть тестовые)")
    void listForAdmin() throws Exception {
        mvc.perform(get(BASE).header("Authorization", adminHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.response[0].id").exists())
                .andExpect(jsonPath("$.response[0].name").exists())
                .andExpect(jsonPath("$.response[0].surname").exists());
    }

    @Test
    @DisplayName("GET список: студент тоже может смотреть")
    void listForStudent() throws Exception {
        mvc.perform(get(BASE).header("Authorization", studentHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("GET список: без токена доступ закрыт (403)")
    void listWithoutLoginIsForbidden() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Испорченный токен: 401 и JSON с ошибкой")
    void brokenTokenGives401() throws Exception {
        mvc.perform(get(BASE).header("Authorization", "Bearer broken.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("error")));
    }

    // POST /save и GET /{id}

    @Test
    @DisplayName("POST save: администратор добавляет студента, карточка содержит phoneNumber и email")
    void adminCreatesStudentAndReadsCard() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(get(BASE + "/" + id).header("Authorization", studentHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.surname").value("Kim"))
                .andExpect(jsonPath("$.phoneNumber").value("+79161234567"))
                .andExpect(jsonPath("$.email").value("anna@kim.ru"));
    }

    @Test
    @DisplayName("POST save: студенту нельзя (403)")
    void studentCannotCreate() throws Exception {
        mvc.perform(post(BASE + "/save")
                        .header("Authorization", studentHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anna\",\"surname\":\"Kim\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST save: без токена нельзя (403)")
    void anonymousCannotCreate() throws Exception {
        mvc.perform(post(BASE + "/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anna\",\"surname\":\"Kim\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id}: нет такого студента, ответ 404 и JSON с сообщением")
    void unknownStudentGives404() throws Exception {
        mvc.perform(get(BASE + "/987654").header("Authorization", adminHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("GET info/{id}: то же самое, что карточка студента")
    void infoEndpoint() throws Exception {
        long id = createStudent("Ivan", "Petrov", "+79990001122", "ivan@petrov.ru");

        mvc.perform(get(BASE + "/info/" + id).header("Authorization", studentHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Petrov"));
    }

    // PUT /{id}

    @Test
    @DisplayName("PUT: администратор меняет телефон и email, имя и фамилия остаются")
    void adminUpdatesPhoneAndEmail() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(put(BASE + "/" + id)
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+79990001122\",\"email\":\"new@kim.ru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mvc.perform(get(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.surname").value("Kim"))
                .andExpect(jsonPath("$.phoneNumber").value("+79990001122"))
                .andExpect(jsonPath("$.email").value("new@kim.ru"));
    }

    @Test
    @DisplayName("PUT: поле, которое не прислали, остаётся прежним")
    void updateWithoutEmailKeepsEmail() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(put(BASE + "/" + id)
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+79990001122\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(jsonPath("$.phoneNumber").value("+79990001122"))
                .andExpect(jsonPath("$.email").value("anna@kim.ru"));
    }

    @Test
    @DisplayName("PUT: студенту нельзя (403)")
    void studentCannotUpdate() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(put(BASE + "/" + id)
                        .header("Authorization", studentHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+79990001122\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(jsonPath("$.phoneNumber").value("+79161234567"));
    }

    @Test
    @DisplayName("PUT: нет такого студента, 404")
    void updateUnknownStudentGives404() throws Exception {
        mvc.perform(put(BASE + "/987654")
                        .header("Authorization", adminHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+79990001122\"}"))
                .andExpect(status().isNotFound());
    }

    // DELETE /{id}

    @Test
    @DisplayName("DELETE: администратор удаляет студента, после этого карточка даёт 404")
    void adminDeletesStudent() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(delete(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: студенту нельзя (403), студент остаётся в базе")
    void studentCannotDelete() throws Exception {
        long id = createStudent("Anna", "Kim", "+79161234567", "anna@kim.ru");

        mvc.perform(delete(BASE + "/" + id).header("Authorization", studentHeader()))
                .andExpect(status().isForbidden());

        mvc.perform(get(BASE + "/" + id).header("Authorization", adminHeader()))
                .andExpect(status().isOk());
    }

    // страницы сайта

    @Test
    @DisplayName("Страницы сайта открываются без токена, а данные закрыты")
    void staticPagesAreOpen() throws Exception {
        mvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("auth-form")));
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/users.html")).andExpect(status().isOk());
    }
}
