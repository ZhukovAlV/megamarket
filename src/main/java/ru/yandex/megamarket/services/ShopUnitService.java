package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ShopUnitService {

    private final ShopUnitRepo shopUnitRepo;
    private final ParserService parserService;

    @Autowired
    public ShopUnitService(ShopUnitRepo shopUnitRepo, ParserService parserService) {
        this.shopUnitRepo = shopUnitRepo;
        this.parserService = parserService;
    }

    /**
     * Импорт товаров и/или категорий
     * @param shopUnitImportRequest запрос со списком товаров и/или категорий
     */
    public void importShopUnitItems(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnit> shopUnits = parserService.parseShopUnitImportRequest(shopUnitImportRequest);
        shopUnitRepo.saveAll(shopUnits);
    }

    /**
     * Получение товара и/или категории по id
     * @param id идентификатор товара и/или категории
     * @return Optional<ShopUnit>
     */
    public Optional<ShopUnit> getShopUnitById(UUID id) {
        return shopUnitRepo.findById(id);
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
        UUID uuid = parserService.stringToUUID(id);
        ShopUnit shopUnit = shopUnitRepo.findById(uuid).orElseThrow(ItemNotFoundException::new);

        // Удаляем ссылку на этот parentId у дочерних объектов
        updateChildrenItem(shopUnit);

        shopUnitRepo.delete(shopUnit);
    }

    /**
     * Выставляем null в parentID удаленного объекта и обновляем дату
     * @param shopUnit объект ShopUnit, который проверяем на наличие children
     */
    private void updateChildrenItem(ShopUnit shopUnit) {
        if (shopUnit.getChildren() != null && shopUnit.getChildren().size() > 0) {
            List<ShopUnit> shopUnitList = shopUnit.getChildren();
            for (ShopUnit currentUnit : shopUnitList) {
                currentUnit.setParentId(null);
                currentUnit.setDate(LocalDateTime.now());
                saveShopUnit(currentUnit);
            }
        }
    }
}
