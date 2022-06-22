package ru.yandex.megamarket.model;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
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
    private String updateDate;
}
