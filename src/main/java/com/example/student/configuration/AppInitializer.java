package com.example.student.configuration;

import com.example.student.model.Role;
import com.example.student.model.Student;
import com.example.student.model.User;
import com.example.student.repository.StudentRepository;
import com.example.student.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AppInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;

    @PostConstruct
    public void init() {
        createUserIfNotExists("admin@example.com", "admin", Set.of(Role.ADMIN));
        createUserIfNotExists("student@example.com", "student", Set.of(Role.STUDENT));
        createStudentsIfEmpty();
    }

    private void createUserIfNotExists(String email, String rawPassword, Set<Role> roles) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRoles(roles);
            userRepository.save(user);
            System.out.println("Создан пользователь c логином: " + email);
        }
    }
    // Тестовые студенты создаются только если таблица пустая (то есть один раз).
    // Первый студент получит ID 1, второй - ID 2. Чтобы изменить список,
    // поменяйте или добавьте строки createStudent(...) ниже
    // (порядок: имя, фамилия, телефон, email; ненужное - null).
    private void createStudentsIfEmpty() {
        if (studentRepository.count() == 0) {
            createStudent("Полина", "Подлозная", "+79161234567", "polina@yandex.ru");
            createStudent("Иван", "Иванов", null, null);

            System.out.println("Созданы тестовые записи студентов.");
        }
    }

    private void createStudent(String name, String surname, String phone, String email) {
        Student student = new Student();
        student.setName(name);
        student.setSurname(surname);
        student.setPhoneNumber(phone);
        student.setEmail(email);
        studentRepository.save(student);
    }
}
