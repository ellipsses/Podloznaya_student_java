package com.example.student.mapper;

import com.example.student.dto.StudentFullResponse;
import com.example.student.dto.StudentRequest;
import com.example.student.dto.StudentResponse;
import com.example.student.dto.StudentResponseForList;
import com.example.student.dto.StudentResponseList;
import com.example.student.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperImplTest {

    private final StudentMapperImpl mapper = new StudentMapperImpl();

    private static Student student(long id, String name, String surname) {
        Student s = new Student();
        s.setId(id);
        s.setName(name);
        s.setSurname(surname);
        s.setPhoneNumber("+79161234567");
        s.setEmail("anna@kim.ru");
        return s;
    }

    @Test
    @DisplayName("Запрос превращается в сущность Student (ID ещё не задан)")
    void requestToStudent() {
        Student s = mapper.requestToStudent(new StudentRequest("Anna", "Kim", "+79161234567", "anna@kim.ru"));

        assertThat(s.getId()).isNull();
        assertThat(s.getName()).isEqualTo("Anna");
        assertThat(s.getSurname()).isEqualTo("Kim");
        assertThat(s.getPhoneNumber()).isEqualTo("+79161234567");
        assertThat(s.getEmail()).isEqualTo("anna@kim.ru");
    }

    @Test
    @DisplayName("Полный ответ содержит все поля студента")
    void studentToFullResponse() {
        StudentFullResponse r = mapper.studentToFullResponse(student(1, "Anna", "Kim"));

        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getName()).isEqualTo("Anna");
        assertThat(r.getSurname()).isEqualTo("Kim");
        assertThat(r.getPhoneNumber()).isEqualTo("+79161234567");
        assertThat(r.getEmail()).isEqualTo("anna@kim.ru");
    }

    @Test
    @DisplayName("Краткий ответ содержит только ID, имя и фамилию")
    void studentToResponse() {
        StudentResponse r = mapper.studentToResponse(student(2, "Ivan", "Ivanov"));

        assertThat(r.getId()).isEqualTo(2L);
        assertThat(r.getName()).isEqualTo("Ivan");
        assertThat(r.getSurname()).isEqualTo("Ivanov");
    }

    @Test
    @DisplayName("Элемент списка содержит ID, имя и фамилию")
    void studentToListItem() {
        StudentResponseForList r = mapper.studentToStudentResponseForList(student(3, "Petr", "Semenov"));

        assertThat(r.getId()).isEqualTo(3L);
        assertThat(r.getName()).isEqualTo("Petr");
        assertThat(r.getSurname()).isEqualTo("Semenov");
    }

    @Test
    @DisplayName("Список студентов превращается в ответ со счётчиком")
    void listToResponseList() {
        StudentResponseList r = mapper.studentListToStudentResponseList(
                List.of(student(1, "Anna", "Kim"), student(2, "Ivan", "Ivanov")));

        assertThat(r.getTotalCount()).isEqualTo(2);
        assertThat(r.getResponse()).extracting(StudentResponseForList::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Пустой список даёт 0 и пустой ответ")
    void emptyList() {
        StudentResponseList r = mapper.studentListToStudentResponseList(List.of());

        assertThat(r.getTotalCount()).isZero();
        assertThat(r.getResponse()).isEmpty();
    }
}
