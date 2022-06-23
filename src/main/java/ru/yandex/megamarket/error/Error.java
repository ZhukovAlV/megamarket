package ru.yandex.megamarket.error;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Error {

    @NotNull
    int code;

    @NotNull
    String message;
}
