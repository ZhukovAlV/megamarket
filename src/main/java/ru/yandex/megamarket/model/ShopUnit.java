package ru.yandex.megamarket.model;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    private LocalDateTime date;

    /**
     * UUID родительской категории
     */
    private String parentId;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShopUnitType type;

    private int price;



}
