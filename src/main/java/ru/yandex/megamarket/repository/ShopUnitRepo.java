package ru.yandex.megamarket.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShopUnitRepo extends CrudRepository<ShopUnit, UUID> {

    List<ShopUnit> findAllByTypeAndLastPriceUpdatedDateBetween(ShopUnitType type, OffsetDateTime startDate, OffsetDateTime endDate);

    List<ShopUnit> findAllByParentId(UUID parentId);

    List<ShopUnit> findAllByType(ShopUnitType type);
}
