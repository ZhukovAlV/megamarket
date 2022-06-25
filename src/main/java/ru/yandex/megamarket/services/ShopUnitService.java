package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitStatisticResponse;
import ru.yandex.megamarket.model.ShopUnitType;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        List<ShopUnit> shopUnits = parser.parseShopUnitImportRequest(shopUnitImportRequest);

        // Заполняем среднюю цену категориям
        for (ShopUnit shopUnit : shopUnits) {
            if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) {
                // Выставляем среднюю цену нашей категории
                shopUnit.setPrice(getAveragePriceFromList(shopUnit.getChildren()));
            }
        }

        shopUnitRepo.saveAll(shopUnits);
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

    public ShopUnitStatisticResponse getSalesStatisticFor24Hour(String strDateTime) {
        ShopUnitStatisticResponse shopUnitStatisticResponse = new ShopUnitStatisticResponse();
        // TODO доделать
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
            shopUnitRepo.deleteAll(shopUnitList);
        }
    }
}
