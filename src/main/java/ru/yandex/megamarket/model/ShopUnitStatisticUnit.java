package ru.yandex.megamarket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sun.istack.NotNull;
import lombok.*;
import ru.yandex.megamarket.services.ParserService;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopUnitStatisticUnit {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    /**
     * Имя элемента
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * UUID родительской категории
     */
    private String parentId;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShopUnitType type;

    /**
     * Целое число, для категории - это средняя цена всех дочерних
     * товаров(включая товары подкатегорий).
     * Если цена является не целым числом, округляется в меньшую сторону
     * до целого числа. Если категория не содержит товаров цена равна null.
     */
    private Long price;

    /**
     * Время последнего обновления элемента.
     */
    @NotNull
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ParserService.FORMAT_DATE_TIME)
    private OffsetDateTime date;
}
