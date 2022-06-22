package ru.yandex.megamarket.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.megamarket.error.Error;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;

@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Невалидная схема документа или входные данные не верны
     * @param ex исключение ValidationFailedException
     * @return Error код ошибки и сообщение
     */
    @ExceptionHandler(ValidationFailedException.class)
    protected ResponseEntity<Error> handleValidationFailed(ValidationFailedException ex) {
        Error error = new Error(400, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Категория/товар не найден
     * @param ex исключение ItemNotFoundException
     * @return Error код ошибки и сообщение
     */
    @ExceptionHandler(ItemNotFoundException.class)
    protected ResponseEntity<Error> handleValidationFailed(ItemNotFoundException ex) {
        Error error = new Error(404, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
