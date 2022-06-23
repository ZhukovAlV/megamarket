package ru.yandex.megamarket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Объект товара или категории
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopUnit {

    @Id
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
    private List<ShopUnit> children;

    public static class Builder {
        private final ShopUnit newShopUnit;

        public Builder() {
            newShopUnit = new ShopUnit();
        }

        public Builder withId(String id){
            newShopUnit.id = id;
            return this;
        }
        public Builder withName(String name){
            newShopUnit.name = name;
            return this;
        }

        public Builder withDate(LocalDateTime date){
            newShopUnit.date = date;
            return this;
        }

        public Builder withParentId(String parentId){
            newShopUnit.parentId = parentId;
            return this;
        }

        public Builder withType(ShopUnitType type){
            newShopUnit.type = type;
            return this;
        }

        public Builder withPrice(Long price){
            newShopUnit.price = price;
            return this;
        }

        public Builder withChildren(List<ShopUnit> children){
            newShopUnit.children = children;
            return this;
        }

        public ShopUnit build(){
            return newShopUnit;
        }
    }
}
