package com.example.student.configuration;

import com.example.student.IntegrationTestBase;
import com.example.student.model.Role;
import com.example.student.model.Student;
import com.example.student.model.User;
import com.example.student.repository.StudentRepository;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AppInitializerTest extends IntegrationTestBase {

    @Autowired
    private AppInitializer initializer;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("При запуске создаются admin (ADMIN) и student (STUDENT)")
    void defaultUsersAreCreated() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        User student = userRepository.findByEmail("student@example.com").orElseThrow();

        assertThat(admin.getRoles()).containsExactly(Role.ADMIN);
        assertThat(student.getRoles()).containsExactly(Role.STUDENT);
    }

    @Test
    @DisplayName("При запуске создаются тестовые студенты, у первого есть телефон и email")
    void defaultStudentsAreCreated() {
        assertThat(studentRepository.count()).isGreaterThanOrEqualTo(2);
        Student first = studentRepository.findAll().stream()
                .filter(s -> "polina@yandex.ru".equals(s.getEmail()))
                .findFirst().orElseThrow();

        assertThat(first.getPhoneNumber()).isEqualTo("+79161234567");
    }

    @Test
    @DisplayName("Повторный запуск init() ничего не дублирует")
    void secondRunDoesNotDuplicate() {
        long users = userRepository.count();
        long students = studentRepository.count();

        initializer.init();

        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(studentRepository.count()).isEqualTo(students);
    }
}
