package ru.yandex.megamarket.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShopUnitRepo extends CrudRepository<ShopUnit, String> {

    List<ShopUnit> findAllByTypeAndDateBetween(ShopUnitType type, LocalDateTime start, LocalDateTime to);
}
