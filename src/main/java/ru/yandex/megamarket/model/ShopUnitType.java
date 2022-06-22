package ru.yandex.megamarket.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *  Тип элемента - категория или товар
 */
@Getter
@RequiredArgsConstructor
public enum ShopUnitType {
    OFFER,
    CATEGORY
}
