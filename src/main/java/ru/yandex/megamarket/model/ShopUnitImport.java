package ru.yandex.megamarket.model;

import com.sun.istack.NotNull;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShopUnitImport {

    @Id
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
    private ShopUnitType type;

    /**
     * Целое число, для категорий поле должно содержать null
     */
    private Long price;
}
