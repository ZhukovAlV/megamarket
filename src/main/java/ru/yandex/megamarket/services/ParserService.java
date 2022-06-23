package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ParserService {

    private static final int LENGTH_UUID = 36;

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
        OffsetDateTime dateUpdate = isIsoDate(shopUnitImportRequest.getUpdateDate());

        List<ShopUnit> listRes = new ArrayList<>();
        for (ShopUnitImport item : shopUnitImportRequest.getItems()) {
            ShopUnit shopUnit = new ShopUnit.Builder()
                    .withId(stringToUUID(item.getId()))
                    .withName(item.getName())
                    .withDate(dateUpdate)
                    .withParentId(stringToUUID(item.getParentId()))
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
     * Преобразование String в UUID
     * @param id идентификатор в String
     * @return идентификатор UUID
     */
    public UUID stringToUUID(String id) {
        if (id == null) return null;
        else if (id.length() != LENGTH_UUID) throw new ValidationFailedException();
        else return UUID.fromString(id);
    }

    /**
     * Наполнение дочерние объекты у родителей
     * @param listShopUnit список объектов ShopUnit
     */
    public void addChildrenToParents(List<ShopUnit> listShopUnit) {
        for (int i = 0; i < listShopUnit.size(); i++) {

            // Для пустой категории поле children равно пустому массиву, а для товара оставляем null
            if (listShopUnit.get(i).getChildren() == null
                    && listShopUnit.get(i).getType().equals(ShopUnitType.CATEGORY)) listShopUnit.get(i).setChildren(new ArrayList<>());

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

            // Название, ТИП элемента не может быть null
            if (shopUnit.getName() == null || shopUnit.getName().isEmpty()
                    || shopUnit.getType() == null) throw new ValidationFailedException();

            // У категорий поле price должно содержать null
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)
                    && shopUnit.getPrice() != null) throw new ValidationFailedException();

            // Цена товара не может быть null и должна быть больше либо равна нулю.
            if (shopUnit.getType().equals(ShopUnitType.OFFER)
                    && (shopUnit.getPrice() == null || shopUnit.getPrice() < 0)) throw new ValidationFailedException();

            // UUID товара или категории является уникальным среди товаров и категорий
            long count = list.stream().filter(item -> shopUnit.getId().equals(item.getId())).count();
            if (count > 1) throw new ValidationFailedException();

            // Родителем товара или категории может быть только категория
            if (shopUnit.getParentId() != null) isParentCategory(list, shopUnit);
        }
    }

    /**
     * Проверяем что родитель это Категория
     * @param list список ShopUnit
     * @param shopUnit объект ShopUnit, parentId которого проверяем
     */
    public void isParentCategory(List<ShopUnit> list, ShopUnit shopUnit) {
        boolean isParentCategory = false;
        for (ShopUnit item : list) {
            if (shopUnit.getParentId().equals(item.getId())
                    && item.getType().equals(ShopUnitType.CATEGORY)) {
                isParentCategory = true;
                break;
            }
        }
        if (!isParentCategory) throw new ValidationFailedException();
    }

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     * @return дата в формате LocalDateTime
     */
    private static OffsetDateTime isIsoDate(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[xxx][xx][X]");
            return OffsetDateTime.parse(date, formatter);
        } catch (DateTimeParseException e) {
            throw new ValidationFailedException();
        }
    }
}
