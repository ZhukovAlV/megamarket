package ru.yandex.megamarket.model;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "ShopUnitImport")
public class ShopUnitImport {

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
    private ShopUnitType type;

    /**
     * Целое число, для категорий поле должно содержать null
     */
    private Long price;
}
