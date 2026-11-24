package com.example.quiz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Конфликт с текущим состоянием данных: дубликат, нельзя удалить и т. п. (HTTP 409). */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}