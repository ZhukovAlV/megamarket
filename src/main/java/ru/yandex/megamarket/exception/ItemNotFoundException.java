package ru.yandex.megamarket.exception;

public class ItemNotFoundException extends RuntimeException {

    private static final String DESCRIPTION = "Item not found";

    public ItemNotFoundException() {
        super(DESCRIPTION);
    }
}
