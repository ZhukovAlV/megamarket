package ru.yandex.megamarket.error;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Error {

    @NotNull
    int code;

    @NotNull
    String message;
}
