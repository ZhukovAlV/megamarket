package ru.yandex.megamarket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.megamarket.services.ParserService;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Объект товара или категории
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopUnit {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ParserService.FORMAT_DATE_TIME)
    private OffsetDateTime date;

    /**
     * Время последнего обновления цены
     */
    @NotNull
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ParserService.FORMAT_DATE_TIME)
    private OffsetDateTime lastPriceUpdatedDate;

    /**
     * UUID родительской категории
     */
    private UUID parentId;

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
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, mappedBy = "parentId")
    private List<ShopUnit> children;

    public static class Builder {
        private final ShopUnit newShopUnit;

        public Builder() {
            newShopUnit = new ShopUnit();
        }

        public Builder withId(UUID id){
            newShopUnit.id = id;
            return this;
        }
        public Builder withName(String name){
            newShopUnit.name = name;
            return this;
        }

        public Builder withDate(OffsetDateTime date){
            newShopUnit.date = date;
            return this;
        }

        public Builder withLastPriceUpdatedDate(OffsetDateTime lastPriceUpdatedDate){
            newShopUnit.lastPriceUpdatedDate = lastPriceUpdatedDate;
            return this;
        }

        public Builder withParentId(UUID parentId){
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
