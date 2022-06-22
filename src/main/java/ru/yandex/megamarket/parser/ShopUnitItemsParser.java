package ru.yandex.megamarket.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.services.ShopUnitService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

        return listRes;
    }

    public void validate(List<ShopUnit> list) {
        for (ShopUnit shopUnit : list) {

            // uuid товара или категории является уникальным среди товаров и категорий
            long count = list.stream().filter(item -> shopUnit.getId().equals(item.getId())).count();
            if (count > 1) throw new ValidationFailedException();

            // родителем товара или категории может быть только категория
            //if (shopUnit.getParentId().equals(shopUnitService.getShopUnitById(getParentId())))
        }
    }
}
