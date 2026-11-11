package com.example.student;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.hikari.schema=students_test_schema",
        "spring.datasource.hikari.connection-init-sql=CREATE SCHEMA IF NOT EXISTS students_test_schema;",
        // таблицы в тестовой схеме создаются заново при запуске тестов и удаляются в конце
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected WebApplicationContext context;

    protected MockMvc mvc;

    @BeforeEach
    protected void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // Настоящий вход через /api/v1/auth/login, возвращает JWT-токен.
    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    // Заголовок Authorization для готового аккаунта admin.
    protected String adminHeader() throws Exception {
        return "Bearer " + login("admin@example.com", "admin");
    }

    // Заголовок Authorization для готового аккаунта student.
    protected String studentHeader() throws Exception {
        return "Bearer " + login("student@example.com", "student");
    }
}
