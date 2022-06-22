package ru.yandex.megamarket.exception;

public class ValidationFailedException extends RuntimeException {

    private static final String DESCRIPTION = "Validation Failed";

    public ValidationFailedException() {
        super(DESCRIPTION);
    }
}
