package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.model.*;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class ShopUnitService {

    private final ShopUnitRepo shopUnitRepo;
    private final ParserService parser;

    @Autowired
    public ShopUnitService(ShopUnitRepo shopUnitRepo, ParserService parser) {
        this.shopUnitRepo = shopUnitRepo;
        this.parser = parser;
    }

    /**
     * Импорт товаров и/или категорий
     * @param shopUnitImportRequest запрос со списком товаров и/или категорий
     */
    public void importShopUnitItems(ShopUnitImportRequest shopUnitImportRequest) {
        // Достаем разово все товары из базы, чтобы по каждому товару не делать запрос
        List<ShopUnit> listShopUnitFromBD = new ArrayList<>();
        shopUnitRepo.findAll().forEach(listShopUnitFromBD::add);

        List<ShopUnit> shopUnits = parser.parseShopUnitImportRequest(shopUnitImportRequest, listShopUnitFromBD);

        // Сохраняем в БД список объектов
        importUnit(shopUnits, listShopUnitFromBD);
    }

    /**
     * Заполнение средней цены
     * @param shopUnits список категорий и товаров на сохранение в БД
     * @param  listShopUnitFromBD текущий список товаров и категорий из базы (до сохранения в базе нового списка)
     */
    public void importUnit(List<ShopUnit> shopUnits, List<ShopUnit> listShopUnitFromBD) {
        Set<UUID> hashSetParentId = new HashSet<>();

        for (ShopUnit shopUnit : shopUnits) {
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) {
                // Выставляем среднюю цену нашей категории
                shopUnit.setPrice(getAveragePriceFromList(shopUnit.getChildren()));
                // Собираем список ID родителей (parentId) товаров, которые были изменены
            } else {
                // Если у товара до сохранения была другая категория, запомним ее (добавим в hashSetParentId) из текущего списка из БД до новых изменений
                if (listShopUnitFromBD.contains(shopUnit)) {
                    UUID uuid = listShopUnitFromBD.get(listShopUnitFromBD.indexOf(shopUnit)).getParentId();
                    if (uuid != null) hashSetParentId.add(uuid);
                }
                // После этого добавим в hashSetParentId родительские категории из обновленной версии объекта
                addParentIdInSet(hashSetParentId, shopUnit.getParentId());
            }
        }

        // Обновляем сперва товары
        shopUnitRepo.saveAll(shopUnits);

        // Дополнительно обновляем среднюю цену РОДИТЕЛЬСКИМ категориям товаров, которые изменялись
        List<ShopUnit> shopCat = new ArrayList<>();
        hashSetParentId.forEach(elem -> shopCat.add(shopUnitRepo.findById(elem).get()));
        if (!shopCat.isEmpty()) importUnit(shopCat, shopUnits);
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
     * Получение товара и/или категории по id
     * @param id идентификатор товара и/или категории в формате UUID
     * @return Optional<ShopUnit>
     */
    public Optional<ShopUnit> getShopUnitById(UUID id) {
        return shopUnitRepo.findById(id);
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
     * Получение товара и/или категории по id
     * @param id идентификатор товара и/или категории в формате String
     * @return Optional<ShopUnit>
     */
    public Optional<ShopUnit> getShopUnitById(String id) {
        return getShopUnitById(parser.stringToUUID(id));
    }

    /**
     * Получение всех товаров и/или категорий
     * @return список ShopUnit
     */
    public List<ShopUnit> getShopUnits() {
        List<ShopUnit> result = new ArrayList<>();
        shopUnitRepo.findAll().forEach(result::add);
        return result;
    }

    /**
     * Поиск товаров, цена которых менялась последние 24 часа
     * @return ShopUnitStatisticResponse со списком товаров
     */
    public ShopUnitStatisticResponse getSalesStatisticFor24Hour() {
        ShopUnitStatisticResponse shopUnitStatisticResponse = new ShopUnitStatisticResponse();

        // Выставляем дату начала и окончания поиска
        OffsetDateTime startDate = OffsetDateTime.now().minus(1, ChronoUnit.DAYS);
        System.out.println(startDate);
        OffsetDateTime endDate = OffsetDateTime.now();
        System.out.println(endDate);

        // Осуществляем поиск, используя Between нашего репозитория
        List<ShopUnit> itemsList = shopUnitRepo.findAllByTypeAndLastPriceUpdatedDateBetween(ShopUnitType.OFFER, startDate, endDate);
        // Заполняем наш shopUnitStatisticResponse объектами ShopUnitStatisticUnit, созданными из itemsList
        itemsList.forEach(elem -> shopUnitStatisticResponse.getItems().add(
                new ShopUnitStatisticUnit(elem.getId(), elem.getName(), elem.getParentId(),
                        elem.getType(), elem.getPrice(), elem.getDate())));

        return shopUnitStatisticResponse;
    }

    /**
     * Сохранение объекта в БД
     * @param shopUnit объект ShopUnit для сохранения
     */
    @Transactional
    public void saveShopUnit(ShopUnit shopUnit) {
        shopUnitRepo.save(shopUnit);
    }

    /**
     * Удаление объекта ShopUnit
     * @param id идентификатор объекта ShopUnit
     */
    public void deleteShopUnitById(String id) {
        UUID uuid = parser.stringToUUID(id);
        ShopUnit shopUnit = shopUnitRepo.findById(uuid).orElseThrow(ItemNotFoundException::new);

        // При удалении категории удаляются все дочерние элементы
        if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) deleteChildrenItem(shopUnit);

        shopUnitRepo.delete(shopUnit);

        // Обновляем у родительской категории среднюю цену после удаления дочернего элемента
        if (shopUnit.getParentId() != null) {
            Optional<ShopUnit> parentShopUnit = shopUnitRepo.findById(shopUnit.getParentId());
            if (parentShopUnit.isPresent() && !parentShopUnit.get().getChildren().isEmpty()) {
                parentShopUnit.get().setPrice(getAveragePriceFromList(parentShopUnit.get().getChildren()));
                shopUnitRepo.save(parentShopUnit.get());
            }
        }
    }

    /**
     * Удаление всех дочерних элементов
     * @param shopUnit объект категории ShopUnit, который проверяем на наличие children
     */
    private void deleteChildrenItem(ShopUnit shopUnit) {
        if (!shopUnit.getChildren().isEmpty()) {
            List<ShopUnit> shopUnitList = shopUnit.getChildren();

            // Если у дочерней категории имеются категории, то их товары тоже удаляем
            for (ShopUnit unit : shopUnitList) {
                if (unit.getType().equals(ShopUnitType.CATEGORY)) deleteChildrenItem(unit);
            }

            shopUnitRepo.deleteAll(shopUnitList);
        }
    }
}
