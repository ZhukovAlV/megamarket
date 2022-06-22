package ru.yandex.megamarket.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.model.*;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.util.*;

@Service
public class ShopUnitService {

    private final ShopUnitRepo shopUnitRepo;

    @Autowired
    public ShopUnitService(ShopUnitRepo shopUnitRepo) {
        this.shopUnitRepo = shopUnitRepo;
    }

    public Optional<ShopUnit> getShopUnitById(String id) {
        return shopUnitRepo.findById(id);
    }

    @Transactional
    public void saveShopUnit(ShopUnit shopUnit) {
        shopUnitRepo.save(shopUnit);
    }

    public List<ShopUnit> getShopUnits() {
        List<ShopUnit> result = new ArrayList<>();
        shopUnitRepo.findAll().forEach(result::add);
        return result;
    }

}
