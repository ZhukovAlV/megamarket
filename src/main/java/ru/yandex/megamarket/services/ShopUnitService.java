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
    private final ImportShopUnitService importShopUnitService;

    @Autowired
    public ShopUnitService(ShopUnitRepo shopUnitRepo, ImportShopUnitService importShopUnitService) {
        this.shopUnitRepo = shopUnitRepo;
        this.importShopUnitService = importShopUnitService;
    }

    /**
     * Импорт товаров и/или категорий
     * @param shopUnitImportRequest запрос со списком товаров и/или категорий
     */
    public void importShopUnitItems(ShopUnitImportRequest shopUnitImportRequest) {
        importShopUnitService.importShopUnitImportRequest(shopUnitImportRequest);
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
     * Получение товара и/или категории по id
     * @param id идентификатор товара и/или категории в формате String
     * @return Optional<ShopUnit>
     */
    public Optional<ShopUnit> getShopUnitById(String id) {
        return getShopUnitById(importShopUnitService.stringToUUID(id));
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
        UUID uuid = importShopUnitService.stringToUUID(id);
        ShopUnit shopUnit = shopUnitRepo.findById(uuid).orElseThrow(ItemNotFoundException::new);

        // При удалении категории удаляются все дочерние элементы
        if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) deleteChildrenItem(shopUnit);

        shopUnitRepo.delete(shopUnit);

        // Обновляем у родительской категории среднюю цену
        importShopUnitService.updateParentCategory(shopUnit);
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
