package ru.yandex.megamarket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Объект товара или категории
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "ShopUnit")
public class ShopUnit {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)")
    private String id;

    /**
     * Имя категории
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * Время последнего обновления элемента
     */
    @NotNull
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime date;

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
     * товаров (включая товары подкатегорий). Если цена является не целым числом,
     * округляется в меньшую сторону до целого числа. Если категория не содержит
     * товаров цена равна null.
     */
    private Long price;

    /**
     * Список всех дочерних товаров\категорий. Для товаров поле равно null.
     */
    @Transient
    private List<ShopUnit> children = new ArrayList<>(); // в другой класс
}
