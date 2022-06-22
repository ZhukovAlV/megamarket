package ru.yandex.megamarket.parser;

import org.springframework.stereotype.Component;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImportRequest;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShopUnitItemsParser {

    public List<ShopUnit> parseShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnit> listRes = new ArrayList<>();


        return listRes;
    }
}
