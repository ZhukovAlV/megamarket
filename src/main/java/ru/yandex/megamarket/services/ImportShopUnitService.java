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
import java.util.*;

@Service
@Slf4j
public class ImportShopUnitService {

    private static final int LENGTH_UUID = 36;

    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]";

    private final ShopUnitRepo shopUnitRepo;

    @Autowired
    public ImportShopUnitService(ShopUnitRepo shopUnitRepo) {
        this.shopUnitRepo = shopUnitRepo;
    }

    /**
     * Парсер для ShopUnitImportRequest
     * @param shopUnitImportRequest запрос с объектами ShopUnitImport
     * @return List<ShopUnit> список объектов ShopUnit
     */
    public List<ShopUnit> importShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest) {

        // Проверка даты на валидность
        OffsetDateTime dateUpdate = getIsoDate(shopUnitImportRequest.getUpdateDate());

        // Создаем множество с UUID из родительских категорий
        Set<UUID> listUUIDParentItem = new HashSet<>();

        // Заполняем список на обновление
        List<ShopUnit> listRes = new ArrayList<>();
        for (ShopUnitImport item : shopUnitImportRequest.getItems()) {
            UUID uuid = stringToUUID(item.getId());

            Optional<ShopUnit> optionalShopUnit = shopUnitRepo.findById(uuid);
            if (optionalShopUnit.isPresent()) {
                // Если в базе есть и это товар
                if (item.getType().equals(ShopUnitType.OFFER)) {
                    ShopUnit shopUnit = new ShopUnit.Builder()
                            .withId(uuid)
                            .withName(item.getName())
                            .withDate(dateUpdate)
                            .withParentId(stringToUUID(item.getParentId()))
                            .withType(item.getType())
                            .withPrice(item.getPrice())
                            .build();
                    // Если менялась цена заполняем дату обновления цены новую, иначе оставляем старую
                    if (optionalShopUnit.get().getPrice().equals(item.getPrice()))
                        if (optionalShopUnit.get().getLastPriceUpdatedDate() != null) shopUnit.setLastPriceUpdatedDate(optionalShopUnit.get().getLastPriceUpdatedDate());
                    else shopUnit.setLastPriceUpdatedDate(dateUpdate);

                    // Добавляем старого и нового родителя в список на обновление средней цены
                    if (optionalShopUnit.get().getParentId() != null) listUUIDParentItem.add(optionalShopUnit.get().getParentId());
                    if (shopUnit.getParentId() != null) listUUIDParentItem.add(shopUnit.getParentId());

                    // Добавляем наш товар в список на обновление
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
                            .withChildren(optionalShopUnit.get().getChildren()) // Берем из базы имеющиеся дочерние товары и категории
                            .build();
                    listRes.add(shopUnit);

                    // Добавляем старого и нового родителя в список на обновление средней цены
                    if (optionalShopUnit.get().getParentId() != null) listUUIDParentItem.add(optionalShopUnit.get().getParentId());
                    if (shopUnit.getParentId() != null) listUUIDParentItem.add(shopUnit.getParentId());
                }
            } else {
                // Если в базе нет, создаем по умолчанию
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

        // Добавление родительских категорий, которые не оказались в списке на обновление, иначе исключение
        List<ShopUnit> listCategory = new ArrayList<>();
        for (UUID uuid : listUUIDParentItem) {
            Optional<ShopUnit> categoryFromList = listRes.stream()
                    .filter(elem -> elem.getId().equals(uuid)).findFirst();
            if (categoryFromList.isEmpty()) {
                Optional<ShopUnit> item = shopUnitRepo.findById(uuid);
                if (item.isPresent()) listCategory.add(item.get());
                else throw new ValidationFailedException();
            }
        }

        // Проверяем корректность данных
        validate(listRes, listCategory);

        // Наполняем дочерние объекты у родителей, поле children
        addChildrenToParents(listRes, listCategory);

        importUnit(listRes);
        importUnit(listCategory);

        return listRes;
    }

    /**
     * Заполнение средней цены
     * @param shopUnits список категорий и товаров на сохранение в БД
     */
    public void importUnit(List<ShopUnit> shopUnits) {
        Set<UUID> hashSetParentId = new HashSet<>();

        for (ShopUnit shopUnit : shopUnits) {
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) {
                // Выставляем среднюю цену нашей категории
                shopUnit.setPrice(getAveragePriceFromList(shopUnit.getChildren()));
                // Собираем список ID родителей (parentId) товаров, которые были изменены
            } else {
                // После этого добавим в hashSetParentId родительские категории из обновленной версии объекта
                addParentIdInSet(hashSetParentId, shopUnit.getParentId());
            }
        }

        // Сохраняем все в базу данных
        shopUnitRepo.saveAll(shopUnits);

        // Дополнительно обновляем среднюю цену РОДИТЕЛЬСКИМ категориям товаров, которые изменялись
        List<ShopUnit> shopCat = new ArrayList<>();
        hashSetParentId.forEach(elem -> shopCat.add(shopUnitRepo.findById(elem).get()));
        if (!shopCat.isEmpty()) importUnit(shopCat);
    }

    /**
     * Обновление у родительской категории товара средней цены
     * @param shopUnit товар
     */
    public void updateParentCategory(ShopUnit shopUnit) {
        if (shopUnit.getParentId() != null) {
            Optional<ShopUnit> parentShopUnit = shopUnitRepo.findById(shopUnit.getParentId());
            if (parentShopUnit.isPresent() && !parentShopUnit.get().getChildren().isEmpty()) {
                parentShopUnit.get().setPrice(getAveragePriceFromList(parentShopUnit.get().getChildren()));
                shopUnitRepo.save(parentShopUnit.get());
            }
        }
    }

    /**
     * Добавление родителя категории, если имеется в список на обновление средней цены
     * @param hashSetParentId SET с категориями на обновление
     * @param uuid uuid категории
     */
    public void addParentIdInSet(Set<UUID> hashSetParentId, UUID uuid) {
        var category = shopUnitRepo.findById(uuid);
        if (category.isPresent() && category.get().getParentId() != null) addParentIdInSet(hashSetParentId, category.get().getParentId());
        hashSetParentId.add(uuid);
    }

    /**
     * Средняя цена всех дочерних элементов категории (+ дочерних элементов самих дочерних элементов)
     * @param listShopUnit Список дочерних элементов категории
     * @return Long средняя цена
     */
    public Long getAveragePriceFromList(List<ShopUnit> listShopUnit) {
        List<Long> listPrice = getPricesFromList(listShopUnit);
        double averagePrice = listPrice.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics().getAverage();
        return Double.valueOf(averagePrice).longValue();
    }

    /**
     * Список цен по категории
     * @param listShopUnit Список товаров и/или категорий
     * @return Список цен по категории
     */
    public List<Long> getPricesFromList(List<ShopUnit> listShopUnit) {
        List<Long> listPrice = new ArrayList<>();
        for (ShopUnit child : listShopUnit) {
            // Если это товар то просто добавляем цену, иначе снова запускаем этот метод
            if (child.getType().equals(ShopUnitType.OFFER)) listPrice.add(child.getPrice());
            else if (!child.getChildren().isEmpty()) {
                List<Long> listChildrenPrice = getPricesFromList(child.getChildren());
                listPrice.addAll(listChildrenPrice);
            }
        }
        return listPrice;
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
    public void addChildrenToParents(List<ShopUnit> listShopUnit, List<ShopUnit> listCategory) {
        // Для пустой категории поле children равно пустому массиву, а для товара оставляем null
        for (int i = 0; i < listShopUnit.size(); i++) {

            if (listShopUnit.get(i).getType().equals(ShopUnitType.CATEGORY)) {
                if (listShopUnit.get(i).getChildren() == null) listShopUnit.get(i).setChildren(new ArrayList<>());

                // Выставляем дочерних детей из массива
                addChildren(listShopUnit, i);
                // То же самое делаем с уже имеющимися объектами категории из БД
                addChildren(listShopUnit, listCategory, i);

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

    public void addChildren(List<ShopUnit> listShopUnit, List<ShopUnit> listCategoryFromBD, int i) {
        for (ShopUnit shopUnitBD : listCategoryFromBD) {
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
    public void validate(List<ShopUnit> list, List<ShopUnit> listCategory) {
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
            if (shopUnit.getParentId() != null) isParentCategory(list, listCategory, shopUnit);
        }
    }

    /**
     * Проверяем что родитель это Категория
     * @param list список ShopUnit
     * @param shopUnit объект ShopUnit, parentId которого проверяем
     */
    public void isParentCategory(List<ShopUnit> list, List<ShopUnit> listCategory, ShopUnit shopUnit) {
        boolean isParentCategory = false;
        // Сперва ищем родителя в списке на импорт
        for (ShopUnit item : list) {
            if (shopUnit.getParentId().equals(item.getId())
                    && item.getType().equals(ShopUnitType.CATEGORY)) {
                isParentCategory = true;
                break;
            }
        }
        // Затем прогоняем по списку из базы данных категорий, если родитель не нашелся
        if (!isParentCategory) {
            for (ShopUnit itemFromBD : listCategory) {
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
