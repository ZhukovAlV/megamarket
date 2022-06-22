package ru.yandex.megamarket.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ShopUnitStatisticResponse {

    /**
     * История в произвольном порядке
     */
    private List<ShopUnitStatisticUnit> items = new ArrayList<>();
}
