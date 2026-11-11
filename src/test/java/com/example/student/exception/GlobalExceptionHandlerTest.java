package com.example.student.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("EntityNotFoundException превращается в ответ 404 с текстом ошибки")
    void notFoundIsMappedTo404() {
        ResponseEntity<AppError> response = new GlobalExceptionHandler()
                .catchEntityNotFoundException(new EntityNotFoundException("Student with ID 7 not found!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Student with ID 7 not found!");
    }
}
