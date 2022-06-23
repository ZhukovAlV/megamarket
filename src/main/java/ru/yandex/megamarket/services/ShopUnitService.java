package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.model.*;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.util.*;

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
    public Optional<ShopUnit> getShopUnitById(String id) {
        return shopUnitRepo.findById(id);
    }

/*    public ShopUnit getInfoOfItemAndItsChildrenById(String id) {
        Optional<ShopUnit> itemOpt = shopUnitRepo.findById(id);
        if (itemOpt.isPresent()) {
            ShopUnit shopUnit = itemOpt.get();
            // TODO Доделать метод findChildrenAsHierarchy
          //  findChildrenAsHierarchy(shopUnit);
            log.info("Информация успешно получена из базы данных");
            return shopUnit;
        }
        log.warn("Данный ID отсутствует в базе данных");
        return null;
    }*/

    @Transactional
    public void saveShopUnit(ShopUnit shopUnit) {
        shopUnitRepo.save(shopUnit);
    }

    public List<ShopUnit> getShopUnits() {
/*        TempShopUnits tempShopUnits = new TempShopUnits();
        List<ShopUnit> result =tempShopUnits.getShopUnitList();*/
        List<ShopUnit> result = new ArrayList<>();
        shopUnitRepo.findAll().forEach(result::add);
        return result;
    }

}
