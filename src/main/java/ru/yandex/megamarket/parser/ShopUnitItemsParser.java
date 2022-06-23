package ru.yandex.megamarket.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitType;
import ru.yandex.megamarket.services.ShopUnitService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ShopUnitItemsParser {

    private final ShopUnitService shopUnitService;

    @Autowired
    public ShopUnitItemsParser(ShopUnitService shopUnitService) {
        this.shopUnitService = shopUnitService;
    }

    public List<ShopUnit> parseShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnit> listRes = new ArrayList<>();
        for (ShopUnitImport item : shopUnitImportRequest.getItems()) {
            ShopUnit shopUnit = new ShopUnit.Builder()
                    .withId(item.getId())
                    .withName(item.getName())
                    .withDate(LocalDateTime.now())
                    .withParentId(item.getParentId())
                    .withType(item.getType())
                    .withPrice(item.getPrice())
                    .build();
            listRes.add(shopUnit);
        }

        // Проверяем корректность данных
        validate(listRes);

        return listRes;
    }

    public void validate(List<ShopUnit> list) {
        for (ShopUnit shopUnit : list) {

            // название элемента не может быть null
            if (shopUnit.getName() == null) throw new ValidationFailedException();

            // у категорий поле price должно содержать null
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)
                    && shopUnit.getPrice() != null) throw new ValidationFailedException();

            // цена товара не может быть null и должна быть больше либо равна нулю.
            if (shopUnit.getType().equals(ShopUnitType.OFFER)
                    && (shopUnit.getPrice() == null || shopUnit.getPrice() < 0)) throw new ValidationFailedException();

            // uuid товара или категории является уникальным среди товаров и категорий
            long count = list.stream().filter(item -> shopUnit.getId().equals(item.getId())).count();
            if (count > 1) throw new ValidationFailedException();

            // родителем товара или категории может быть только категория (смотрим также в базе родителя)
            ShopUnit shopUnitParent;
            if (list.contains(shopUnit)) {
                shopUnitParent = list.get(list.indexOf(shopUnit));
            } else if (shopUnitService.getShopUnitById(shopUnit.getParentId()).isPresent()) {
                shopUnitParent =  shopUnitService.getShopUnitById(shopUnit.getParentId()).get();
            } else throw new ItemNotFoundException();
            if (!shopUnitParent.getType().equals(ShopUnitType.CATEGORY)) throw new ValidationFailedException();
        }
    }

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     */
    private static void isIsoDate(String date) {
        try {
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
        } catch (DateTimeParseException e) {
            throw new ValidationFailedException();
        }
    }
}
