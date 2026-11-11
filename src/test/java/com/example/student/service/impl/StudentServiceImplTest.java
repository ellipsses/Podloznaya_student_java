package com.example.student.service.impl;

import com.example.student.IntegrationTestBase;
import com.example.student.dto.StudentFullResponse;
import com.example.student.dto.StudentRequest;
import com.example.student.dto.StudentResponse;
import com.example.student.dto.StudentUpdateRequest;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Сервис работает с настоящим репозиторием и временной базой, заглушек нет. */
class StudentServiceImplTest extends IntegrationTestBase {

    @Autowired
    private StudentService service;

    private StudentResponse saveAnna() {
        return service.save(new StudentRequest("Anna", "Kim", "+79161234567", "anna@kim.ru"));
    }

    @Test
    @DisplayName("save: студент сохраняется и получает ID")
    void saveReturnsIdAndNames() {
        StudentResponse saved = saveAnna();

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEqualTo("Anna");
        assertThat(saved.getSurname()).isEqualTo("Kim");
    }

    @Test
    @DisplayName("findById: возвращает все сохранённые поля")
    void findByIdReturnsAllFields() {
        long id = saveAnna().getId();

        StudentFullResponse card = service.findById(id);

        assertThat(card.getName()).isEqualTo("Anna");
        assertThat(card.getSurname()).isEqualTo("Kim");
        assertThat(card.getPhoneNumber()).isEqualTo("+79161234567");
        assertThat(card.getEmail()).isEqualTo("anna@kim.ru");
    }

    @Test
    @DisplayName("findById: если студента нет, EntityNotFoundException")
    void findByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> service.findById(987654L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("findAll: после добавления студентов их становится на один больше")
    void findAllCountsNewStudent() {
        int before = service.findAll().getTotalCount();

        saveAnna();

        assertThat(service.findAll().getTotalCount()).isEqualTo(before + 1);
        assertThat(service.findAll().getResponse()).hasSize(before + 1);
    }

    @Test
    @DisplayName("update: меняет телефон и email, имя и фамилию не трогает")
    void updateChangesPhoneAndEmail() {
        long id = saveAnna().getId();

        service.update(id, new StudentUpdateRequest("+79990001122", "new@kim.ru"));

        StudentFullResponse card = service.findById(id);
        assertThat(card.getPhoneNumber()).isEqualTo("+79990001122");
        assertThat(card.getEmail()).isEqualTo("new@kim.ru");
        assertThat(card.getName()).isEqualTo("Anna");
        assertThat(card.getSurname()).isEqualTo("Kim");
    }

    @Test
    @DisplayName("update: поля со значением null остаются прежними")
    void updateKeepsFieldsWhenNull() {
        long id = saveAnna().getId();

        service.update(id, new StudentUpdateRequest(null, null));

        StudentFullResponse card = service.findById(id);
        assertThat(card.getPhoneNumber()).isEqualTo("+79161234567");
        assertThat(card.getEmail()).isEqualTo("anna@kim.ru");
    }

    @Test
    @DisplayName("update: нет такого студента, EntityNotFoundException")
    void updateThrowsWhenMissing() {
        assertThatThrownBy(() -> service.update(987654L, new StudentUpdateRequest("+7", "a@b.ru")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById: после удаления студента больше нет")
    void deleteRemovesStudent() {
        long id = saveAnna().getId();

        service.deleteById(id);

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(EntityNotFoundException.class);
    }
}
