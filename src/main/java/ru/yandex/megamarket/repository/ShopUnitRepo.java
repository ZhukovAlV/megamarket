package ru.yandex.megamarket.repository;

import org.springframework.data.repository.CrudRepository;
import ru.yandex.megamarket.model.ShopUnit;

public interface ShopUnitRepo extends CrudRepository<ShopUnit, String> {
}
