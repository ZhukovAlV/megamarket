package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.model.*;
import ru.yandex.megamarket.parser.ShopUnitItemsParser;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.util.*;

@Service
@Slf4j
public class ShopUnitService {

    private final ShopUnitRepo shopUnitRepo;
    private final ShopUnitItemsParser shopUnitItemsParser;

    @Autowired
    public ShopUnitService(ShopUnitRepo shopUnitRepo, ShopUnitItemsParser shopUnitItemsParser) {
        this.shopUnitRepo = shopUnitRepo;
        this.shopUnitItemsParser = shopUnitItemsParser;
    }

    public void importShopUnitItems(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnit> shopUnits = shopUnitItemsParser.parseShopUnitImportRequest(shopUnitImportRequest);
        shopUnitRepo.saveAll(shopUnits);
    }

/*    public boolean importItemsShopUnit(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnitImport> shopUnitImports = shopUnitImportRequest.getShopUnits();
        boolean validated = validateShopUnitsWhileImport(shopUnitImports);
        if (!validated) {
            log.warn("ITEMS ARE NOT VALIDATED");
            return false;
        }
        String updateDate = shopUnitImportRequest.getUpdateDate();

        for (ShopUnitImport shopUnitToUpdate : shopUnitImports) {
            Optional<ShopUnit> existingItemOpt = ShopUnitService.findById(shopUnitToUpdate.getId());
            if (existingItemOpt.isPresent()) {
                ShopUnit su = existingItemOpt.get();
                if (su.getType().equals(shopUnitToUpdate.getType())) {
                    su.setName(shopUnitToUpdate.getName());
                    su.setDate(updateDate);
                    su.setPrice(shopUnitToUpdate.getPrice());
                    su.setParentId(shopUnitToUpdate.getParentId());
                    ShopUnitService.save(su);
                } else {
                    log.warn("UNEQUAL TYPES");
                    return false;
                }
            } else {
                ShopUnit su = new ShopUnit(shopUnitToUpdate);
                String parentId = shopUnitToUpdate.getParentId();
                if (parentId!=null) {
                    ShopUnitService.findById(parentId).ifPresent(item ->
                            relationRepository.save(
                                    new Relation(parentId, shopUnitToUpdate.getId()))
                    );
                }
                su.setDate(updateDate);
                ShopUnitService.save(su);
            }
            Optional<ShopUnit> itemToFindAveragePrice = ShopUnitService.findById(getHighestParentId(shopUnitToUpdate.getId()));
            if (itemToFindAveragePrice.isPresent()) {
                setAveragePriceOfCategory(itemToFindAveragePrice.get().getId());
            } else {
                throw new RuntimeException("Item doesn't exist");
            }
        }
        log.info("items updated");
        return true;
    }*/


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

    public Optional<ShopUnit> getShopUnitById(String id) {
        return shopUnitRepo.findById(id);
    }

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
