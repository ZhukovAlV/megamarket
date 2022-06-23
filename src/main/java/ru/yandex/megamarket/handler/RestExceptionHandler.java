package ru.yandex.megamarket.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public Error handleValidationFailed(ValidationFailedException ex) {
        return new Error(400, ex.getMessage());
    }

    /**
     * Категория/товар не найден
     * @param ex исключение ItemNotFoundException
     * @return Error код ошибки и сообщение
     */
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public Error handleValidationFailed(ItemNotFoundException ex) {
        return new Error(404, ex.getMessage());
    }
}
