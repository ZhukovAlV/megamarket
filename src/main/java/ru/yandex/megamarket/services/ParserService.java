package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ParserService {

/*    private final ShopUnitRepo shopUnitRepo;

    @Autowired
    public ParserService(ShopUnitRepo shopUnitRepo) {
        this.shopUnitRepo = shopUnitRepo;
    }*/

    /**
     * Парсер для ShopUnitImportRequest
     * @param shopUnitImportRequest запрос с объектами ShopUnitImport
     * @return List<ShopUnit> список объектов ShopUnit
     */
    public List<ShopUnit> parseShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest) {

        // Проверка даты на валидность
        LocalDateTime dateUpdate = isIsoDate(shopUnitImportRequest.getUpdateDate());

        List<ShopUnit> listRes = new ArrayList<>();
        for (ShopUnitImport item : shopUnitImportRequest.getItems()) {
            ShopUnit shopUnit = new ShopUnit.Builder()
                    .withId(item.getId())
                    .withName(item.getName())
                    .withDate(dateUpdate)
                    .withParentId(item.getParentId())
                    .withType(item.getType())
                    .withPrice(item.getPrice())
                    .build();
            listRes.add(shopUnit);
        }

        // Проверяем корректность данных
        validate(listRes);

        // Наполняем дочерние объекты у родителей, поле children
        addChildrenToParents(listRes);

        return listRes;
    }

    /**
     * Наполнение дочерние объекты у родителей
     * @param listShopUnit список объектов ShopUnit
     */
    public void addChildrenToParents(List<ShopUnit> listShopUnit) {
        for (int i = 0; i < listShopUnit.size(); i++) {
            for (ShopUnit shopUnit : listShopUnit) {
                if (shopUnit.getParentId() != null
                        && listShopUnit.get(i).getId().equals(shopUnit.getParentId())) {
                    // Если объект НЕ КАТЕГОРИЯ имеет детей выдаем ошибку иначе добавляем ему ребенка
                    if (!listShopUnit.get(i).getType().equals(ShopUnitType.CATEGORY))
                        throw new ValidationFailedException();
                    else listShopUnit.get(i).getChildren().add(shopUnit);
                }
            }
        }
    }

    /**
     * Валидация данных
     * @param list список ShopUnit
     */
    public void validate(List<ShopUnit> list) {
        // Проверка массива на содержание объектов
        if (list.size() < 1) throw new ItemNotFoundException();

        for (ShopUnit shopUnit : list) {

            // ID, Название, ТИП элемента не может быть null
            if (shopUnit.getId() == null || shopUnit.getName() == null || shopUnit.getType() == null)
                throw new ValidationFailedException();

            // У категорий поле price должно содержать null
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)
                    && shopUnit.getPrice() != null) throw new ValidationFailedException();

            // Цена товара не может быть null и должна быть больше либо равна нулю.
            if (shopUnit.getType().equals(ShopUnitType.OFFER)
                    && (shopUnit.getPrice() == null || shopUnit.getPrice() < 0)) throw new ValidationFailedException();

            // UUID товара или категории является уникальным среди товаров и категорий
            long count = list.stream().filter(item -> shopUnit.getId().equals(item.getId())).count();
            if (count > 1) throw new ValidationFailedException();

            // Родителем товара или категории может быть только категория (смотрим также в базе родителя)
            // TODO Смотреть каждый раз родителя в базе данных накладно для каждого объекта, написал письмо в Яндекс про это
            ShopUnit shopUnitParent;
            if (list.contains(shopUnit)) {
                shopUnitParent = list.get(list.indexOf(shopUnit));
            } /*else if (shopUnitRepo.findById(shopUnit.getParentId()).isPresent()) {
                shopUnitParent =  shopUnitRepo.findById(shopUnit.getParentId()).get();
            }*/ else throw new ItemNotFoundException();
            if (!shopUnitParent.getType().equals(ShopUnitType.CATEGORY)) throw new ValidationFailedException();
        }
    }

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     * @return дата в формате LocalDateTime
     */
    private static LocalDateTime isIsoDate(String date) {
        try {
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
            return LocalDateTime.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationFailedException();
        }
    }
}