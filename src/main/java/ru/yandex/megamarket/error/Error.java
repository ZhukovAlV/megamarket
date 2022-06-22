package ru.yandex.megamarket.error;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;

import javax.persistence.Column;

@AllArgsConstructor
public class Error {

    @NotNull
    int code;

    @NotNull
    String message;
}
