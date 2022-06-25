package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitType;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ParserService {

    private static final int LENGTH_UUID = 36;

    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]";

    private final ShopUnitRepo shopUnitRepo;

    @Autowired
    public ParserService(ShopUnitRepo shopUnitRepo) {
        this.shopUnitRepo = shopUnitRepo;
    }

    /**
     * Парсер для ShopUnitImportRequest
     * @param shopUnitImportRequest запрос с объектами ShopUnitImport
     * @return List<ShopUnit> список объектов ShopUnit
     */
    public List<ShopUnit> parseShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest, List<ShopUnit> listShopUnitFromBD) {

        // Проверка даты на валидность
        OffsetDateTime dateUpdate = getIsoDate(shopUnitImportRequest.getUpdateDate());

        // Создаем список только с UUID для более быстрого поиска по UUID
        List<UUID> listUUID = listShopUnitFromBD.stream().map(ShopUnit::getId).toList();

        List<ShopUnit> listRes = new ArrayList<>();
        for (ShopUnitImport item : shopUnitImportRequest.getItems()) {
            UUID uuid = stringToUUID(item.getId());
            boolean unitExistInDB = listUUID.contains(uuid);

            // Если такого товара в базе нет то создаем новый
            if (!unitExistInDB && item.getType().equals(ShopUnitType.OFFER)) {
                ShopUnit shopUnit = new ShopUnit.Builder()
                        .withId(uuid)
                        .withName(item.getName())
                        .withDate(dateUpdate)
                        .withLastPriceUpdatedDate(dateUpdate)
                        .withParentId(stringToUUID(item.getParentId()))
                        .withType(item.getType())
                        .withPrice(item.getPrice())
                        .build();
                listRes.add(shopUnit);
            // Иначе проверяем была ли изменена цена и создаем товар с определенной датой изменения цены
            } else if (item.getType().equals(ShopUnitType.OFFER)) {
                Optional<ShopUnit> shopUnitFromBD = listShopUnitFromBD.stream()
                        .filter(elem -> elem.getId().equals(uuid)).findFirst();

                // Если цена не менялась дату изменения цены оставляем прежней
                OffsetDateTime updatePriceDate;
                if (shopUnitFromBD.get().getPrice().equals(item.getPrice())) {
                    updatePriceDate = shopUnitFromBD.get().getLastPriceUpdatedDate();
                } else updatePriceDate = dateUpdate;

                ShopUnit shopUnit = new ShopUnit.Builder()
                        .withId(uuid)
                        .withName(item.getName())
                        .withDate(dateUpdate)
                        .withLastPriceUpdatedDate(updatePriceDate)
                        .withParentId(stringToUUID(item.getParentId()))
                        .withType(item.getType())
                        .withPrice(item.getPrice())
                        .build();
                listRes.add(shopUnit);
            } else {
                // Если это категория, то дату изменения цены не заполняем
                ShopUnit shopUnit = new ShopUnit.Builder()
                        .withId(uuid)
                        .withName(item.getName())
                        .withDate(dateUpdate)
                        .withParentId(stringToUUID(item.getParentId()))
                        .withType(item.getType())
                        .withPrice(item.getPrice())
                        .build();
                listRes.add(shopUnit);
            }
        }

        // Проверяем корректность данных
        validate(listRes, listShopUnitFromBD);

        // Наполняем дочерние объекты у родителей, поле children
        addChildrenToParents(listRes, listShopUnitFromBD);

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
    public void addChildrenToParents(List<ShopUnit> listShopUnit, List<ShopUnit> listShopUnitFromBD) {
        // Для пустой категории поле children равно пустому массиву, а для товара оставляем null
        for (int i = 0; i < listShopUnit.size(); i++) {

            if (listShopUnit.get(i).getType().equals(ShopUnitType.CATEGORY)) {
                if (listShopUnit.get(i).getChildren() == null) listShopUnit.get(i).setChildren(new ArrayList<>());

                // Выставляем дочерних детей из массива
                addChildren(listShopUnit, i);
                // То же самое делаем с уже имеющимися объектами из БД
                addChildren(listShopUnit, listShopUnitFromBD, i);

            // Если объект НЕ КАТЕГОРИЯ и имеет детей выдаем ошибку
            } else if ((listShopUnit.get(i).getType().equals(ShopUnitType.OFFER)
                    && listShopUnit.get(i).getChildren() != null)) throw new ValidationFailedException();
        }
    }

    /**
     * Добавление дочерних объектов список объектов ShopUnit из списка и из БД
     * @param listShopUnit список объектов ShopUnit
     * @param i Порядковый номер объекта ShopUnit, в который добавляем дочерние объекты
     */
    public void addChildren(List<ShopUnit> listShopUnit, int i) {
        for (ShopUnit shopUnit : listShopUnit) {
            if (shopUnit.getParentId() != null
                    && listShopUnit.get(i).getId().equals(shopUnit.getParentId())) {
                listShopUnit.get(i).getChildren().add(shopUnit);
            }
        }
    }

    public void addChildren(List<ShopUnit> listShopUnit, List<ShopUnit> listShopUnitFromBD, int i) {
        for (ShopUnit shopUnitBD : listShopUnitFromBD) {
            if (shopUnitBD.getParentId() != null
                    && listShopUnit.get(i).getId().equals(shopUnitBD.getParentId())) {
                listShopUnit.get(i).getChildren().add(shopUnitBD);
            }
        }
    }

    /**
     * Валидация данных
     * @param list список ShopUnit
     */
    public void validate(List<ShopUnit> list, List<ShopUnit> listShopUnitFromBD) {
        // Проверка массива на содержание объектов
        if (list.size() < 1) throw new ItemNotFoundException();

        for (ShopUnit shopUnit : list) {

            // Название, ТИП элемента не может быть null
            if (shopUnit.getName() == null || shopUnit.getName().isEmpty()
                    || shopUnit.getType() == null) throw new ValidationFailedException();

            // У категорий поле price должно содержать null
/*            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)
                    && shopUnit.getPrice() != null) throw new ValidationFailedException();*/

            // Цена товара не может быть null и должна быть больше либо равна нулю.
            if (shopUnit.getType().equals(ShopUnitType.OFFER)
                    && (shopUnit.getPrice() == null || shopUnit.getPrice() < 0)) throw new ValidationFailedException();

            // UUID товара или категории является уникальным среди товаров и категорий
            long count = list.stream().filter(item -> shopUnit.getId().equals(item.getId())).count();
            if (count > 1) throw new ValidationFailedException();

            // Родителем товара или категории может быть только категория
            if (shopUnit.getParentId() != null) isParentCategory(list, listShopUnitFromBD, shopUnit);
        }
    }

    /**
     * Проверяем что родитель это Категория
     * @param list список ShopUnit
     * @param shopUnit объект ShopUnit, parentId которого проверяем
     */
    public void isParentCategory(List<ShopUnit> list, List<ShopUnit> listShopUnitFromBD, ShopUnit shopUnit) {
        boolean isParentCategory = false;
        // Сперва ищем родителя в списке на импорт
        for (ShopUnit item : list) {
            if (shopUnit.getParentId().equals(item.getId())
                    && item.getType().equals(ShopUnitType.CATEGORY)) {
                isParentCategory = true;
                break;
            }
        }
        // Затем прогоняем по списку из базы данных, если родитель не нашелся
        if (!isParentCategory) {
            for (ShopUnit itemFromBD : listShopUnitFromBD) {
                if (shopUnit.getParentId().equals(itemFromBD.getId())
                        && itemFromBD.getType().equals(ShopUnitType.CATEGORY)) {
                    isParentCategory = true;
                    break;
                }
            }
        }

        if (!isParentCategory) throw new ValidationFailedException();
    }

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     * @return дата в формате LocalDateTime
     */
    public OffsetDateTime getIsoDate(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
            return OffsetDateTime.parse(date, formatter);
        } catch (DateTimeParseException e) {
            throw new ValidationFailedException();
        }
    }
}
