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

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "ShopUnitStatisticUnit")
public class ShopUnitStatisticUnit {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)")
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime date;
}
