package ru.yandex.megamarket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ShopUnitImportRequest {

    /**
     * Импортируемые элементы
     */
    @NotNull
    private List<ShopUnitImport> items;

    /**
     * Время обновления добавляемых товаров/категорий
     */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime updateDate;
}
