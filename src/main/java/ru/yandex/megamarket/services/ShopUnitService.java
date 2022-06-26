package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.*;
import ru.yandex.megamarket.repository.ShopUnitRepo;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        validateShopUnitImportRequest(shopUnitImportRequest);
        List<ShopUnitImport> shopUnitImportList = shopUnitImportRequest.getItems();
        validateShopUnitImportList(shopUnitImportList);

        OffsetDateTime updateDate = parserService.getIsoDate(shopUnitImportRequest.getUpdateDate());
        List<ShopUnit> shopUnitsForUpdateInBD = new ArrayList<>();

        for (ShopUnitImport shopUnitImport : shopUnitImportList) {
            Optional<ShopUnit> shopUnitOp = shopUnitRepo.findById(parserService.stringToUUID(shopUnitImport.getId()));
            if (shopUnitOp.isPresent()) {
                ShopUnit shopUnit = shopUnitOp.get();
                if (shopUnit.getType().equals(shopUnitImport.getType())) {
                    shopUnit.setName(shopUnitImport.getName());
                    shopUnit.setDate(updateDate);
                    shopUnit.setPrice(shopUnitImport.getPrice());

                    UUID parentId = parserService.stringToUUID(shopUnitImport.getParentId());
                    if (parentId != null) checkParentId(parentId, shopUnitImportList);
                    shopUnit.setParentId(parentId);
                    shopUnitsForUpdateInBD.add(shopUnit);
                } else {
                    log.warn("Изменение типа элемента с товара на категорию или с категории на товар не допускается");
                    throw new ValidationFailedException();
                }
            } else {
                ShopUnit shopUnit = new ShopUnit.Builder()
                        .withId(parserService.stringToUUID(shopUnitImport.getId()))
                        .withName(shopUnitImport.getName())
                        .withParentId(parserService.stringToUUID(shopUnitImport.getParentId()))
                        .withType(shopUnitImport.getType())
                        .build();
                if (shopUnitImport.getType().equals(ShopUnitType.OFFER)) {
                    shopUnit.setPrice(shopUnitImport.getPrice());
                }

                UUID parentId = parserService.stringToUUID(shopUnitImport.getParentId());
                if (parentId != null) checkParentId(parentId, shopUnitImportList);
                shopUnit.setDate(updateDate);
                shopUnitsForUpdateInBD.add(shopUnit);
            }
        }
        // сохранение в базу данных из импорта
        saveShopUnitList(shopUnitsForUpdateInBD);

        // обновление списков дочерних товаров\категорий
        addChildren(shopUnitsForUpdateInBD, updateDate);

        // обновление цены категорий
        updatePriceOfCategory(updateDate);

        log.info("Вставка или обновление прошли успешно");
    }

    /**
     * Проверка списка shopUnitImportRequest
     * @param shopUnitImportRequest список shopUnitImportRequest
     */
    public void validateShopUnitImportRequest(ShopUnitImportRequest shopUnitImportRequest) {
        List<ShopUnitImport> shopUnitImportList = shopUnitImportRequest.getItems();
        if (shopUnitImportList == null || shopUnitImportList.size() < 1) {
            log.warn("Запрос на импорт содержит пустой список");
            throw new ValidationFailedException();
        }

        for (ShopUnitImport shopUnitImport : shopUnitImportList) {
            long count = shopUnitImportList.stream().filter(item -> shopUnitImport.getId().equals(item.getId())).count();
            if (count > 1) {
                log.warn("В одном запросе не может быть двух элементов с одинаковым id");
                throw new ValidationFailedException();
            }
        }
    }

    /**
     * Проверка списка shopUnitImportList
     * @param shopUnitImportList список shopUnitImportList
     */
    public void validateShopUnitImportList(List<ShopUnitImport> shopUnitImportList) {
        boolean isValidated = true;
        for (ShopUnitImport shopUnitImport : shopUnitImportList) {
            if (shopUnitImport.getId() == null || shopUnitImport.getId().trim().equals("")) {
                log.warn("Поле id не должно быть пустым");
                isValidated = false;
                break;
            }
            if (shopUnitImport.getName() == null
                    || shopUnitImport.getName().trim().equals("")) {
                log.warn("Поле name не должно быть пустым");
                isValidated = false;
                break;
            }
            if (shopUnitImport.getType().equals(ShopUnitType.OFFER)
                    && (shopUnitImport.getPrice() == null || shopUnitImport.getPrice() < 0)) {
                log.warn("Цена товара должна быть больше либо равна нулю");
                isValidated = false;
                break;
            }
            if (shopUnitImport.getType().equals(ShopUnitType.CATEGORY)
                    && shopUnitImport.getPrice() != null) {
                log.warn("У категорий цена должна быть null");
                isValidated = false;
                break;
            }
            if (shopUnitImport.getType() == null) {
                log.warn("Обязательно нужно указывать тип (Категория или товар)");
                isValidated = false;
                break;
            }
        }

        if (isValidated) {
            log.info("Схема импорта элементов валидна");
        } else throw new ValidationFailedException();
    }

    /**
     * Обновление средней цены категорий
     * @param updateDate дата обновления в БД
     */
    private void updatePriceOfCategory(OffsetDateTime updateDate) {
        log.info("Расчет средней цены для категорий");
        List<ShopUnit> shopUnitList = shopUnitRepo.findAllByType(ShopUnitType.CATEGORY);
        for (ShopUnit shopUnit : shopUnitList) {
            Long oldPrice = shopUnit.getPrice();
            Long updatePrice = getAveragePriceFromList(shopUnit.getChildren());
            shopUnit.setPrice(updatePrice);
            if (oldPrice != null && updatePrice != null && !oldPrice.equals(updatePrice)) {
                shopUnit.setDate(updateDate);
            }
        }
        saveShopUnitList(shopUnitList);
    }

    /**
     * Получение средней цены для категории
     * @param children Список всех дочерних товаров/категорий
     * @return Long средняя цена всех дочерних товаров(включая товары подкатегорий)
     */
    public Long getAveragePriceFromList(List<ShopUnit> children) {
        if (children.isEmpty()) {
            return null;
        }

        List<Long> listPrice = getPricesFromChildren(children);
        double averagePrice = listPrice.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics().getAverage();
        return Double.valueOf(averagePrice).longValue();
    }

    /**
     * Получение списка цен по категории
     * @param children Список дочерних товаров/категорий
     * @return Список цен всех дочерних товаров данной категории (включая товары подкатегорий)
     */
    public List<Long> getPricesFromChildren(List<ShopUnit> children) {
        List<Long> childrenPrices = new ArrayList<>();

        for (ShopUnit child : children) {
            if (child.getType().equals(ShopUnitType.OFFER)) {
                childrenPrices.add(child.getPrice());
            } else if (!child.getChildren().isEmpty()) {
                List<Long> childrenPricesOfSubcategory = getPricesFromChildren(child.getChildren());
                childrenPrices.addAll(childrenPricesOfSubcategory);
            }
        }
        return childrenPrices;
    }

    /**
     * Проверка parentId (относится к категории или нет)
     * @param parentId id родительской категории
     * @param shopUnitImportList список для импорта
     */
    private void checkParentId(UUID parentId, List<ShopUnitImport> shopUnitImportList) {
        boolean isCategory = false;
        Optional<ShopUnit> shopUnitOp = shopUnitRepo.findById(parentId);
        if (shopUnitOp.isPresent()
                && shopUnitOp.get().getType().equals(ShopUnitType.CATEGORY)) {
            isCategory = true;
        }
        for (ShopUnitImport item : shopUnitImportList) {
            if (parentId.equals(parserService.stringToUUID(item.getId()))
                    && item.getType().equals(ShopUnitType.CATEGORY)) {
                isCategory = true;
                break;
            }
        }
        if (!isCategory) {
            log.warn("ParentId должен ссылаться на категорию, а не товар");
            throw new ValidationFailedException();
        }
    }

    /**
     * Добавление дочерних объектов
     * @param shopUnitsForUpdateInBD Список для обновления
     * @param updateDate дата обновления
     */
    private void addChildren(List<ShopUnit> shopUnitsForUpdateInBD, OffsetDateTime updateDate) {
        log.info("Добавление дочерних объектов");
        for (ShopUnit shopUnit : shopUnitsForUpdateInBD) {
            addChildrenToItem(shopUnit, updateDate);
            saveShopUnit(shopUnit);
        }
    }

    /**
     * Добавление дочерних объектов по одному элементу
     * @param shopUnit   объект из импорта для сохранения в БД
     * @param updateDate дата обновления в БД
     */
    private void addChildrenToItem(ShopUnit shopUnit, OffsetDateTime updateDate) {
        if (shopUnit.getType().equals(ShopUnitType.OFFER)) {
            log.info("Обновление OFFER " + shopUnit.getName());
            shopUnit.setChildren(null);
        }

        if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) {
            log.info("Обновление категории " + shopUnit.getName());
            Optional<ShopUnit> shopUnitOp = shopUnitRepo.findById(shopUnit.getId());
            if (shopUnitOp.isPresent()) {
                List<ShopUnit> childrenFromBD = new ArrayList<>(shopUnitRepo.findAllByParentId(shopUnit.getId())); // список детей из БД
                shopUnit.setChildren(childrenFromBD);
            }
        }

        if (shopUnit.getParentId() != null) {
            Optional<ShopUnit> parentOp = shopUnitRepo.findById(shopUnit.getParentId());
            if (parentOp.isPresent()) {
                ShopUnit parent = parentOp.get();
                parent.setDate(updateDate);
                addChildrenToItem(parent, updateDate);
            }
        }
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
     * Сохранение списка объектов в БД
     * @param shopUnits список объектов товара и/или категории для сохранения
     */
    @Transactional
    public void saveShopUnitList(List<ShopUnit> shopUnits) {
        log.info("Сохранение списка товаров/категорий");
        shopUnitRepo.saveAll(shopUnits);
    }

    /**
     * Получение товара и/или категории по id
     * @param id идентификатор товара и/или категории в формате String
     * @return Optional<ShopUnit>
     */
    public Optional<ShopUnit> getShopUnitById(String id) {
        return getShopUnitById(parserService.stringToUUID(id));
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
    public ShopUnitStatisticResponse getSalesStatisticFor24Hour(String stringDate) {
        checkDateFormat(stringDate);
        OffsetDateTime endDate = parserService.getIsoDate(stringDate);
        ShopUnitStatisticResponse shopUnitStatisticResponse = new ShopUnitStatisticResponse();

        // Выставляем дату начала и окончания поиска
        OffsetDateTime startDate = endDate.minus(1, ChronoUnit.DAYS);

        // Осуществляем поиск, используя Between нашего репозитория
        List<ShopUnit> itemsList = shopUnitRepo.findAllByTypeAndLastPriceUpdatedDateBetween(ShopUnitType.OFFER, startDate, endDate);
        // Заполняем наш shopUnitStatisticResponse объектами ShopUnitStatisticUnit, созданными из itemsList
        itemsList.forEach(elem -> shopUnitStatisticResponse.getItems().add(
                new ShopUnitStatisticUnit(elem.getId(), elem.getName(), elem.getParentId(),
                        elem.getType(), elem.getPrice(), elem.getDate())));

        return shopUnitStatisticResponse;
    }

    /**
     * Проверка формата даты
     * @param stringDate Дата в текстовом формате
     */
    private void checkDateFormat(String stringDate) {
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z");
        Matcher matcher = pattern.matcher(stringDate);
        if (!matcher.matches()) {
            log.error("Дата имеет не корректный формат");
            throw new ValidationFailedException();
        }
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

        // При удалении категории удаляются все дочерние элементы
        if (shopUnit.getType().equals(ShopUnitType.CATEGORY)) deleteChildrenItem(shopUnit);

        shopUnitRepo.delete(shopUnit);

        // Обновляем у родительской категории среднюю цену
        updateParentCategory(shopUnit);
    }

    /**
     * Обновление у родительской категории товара средней цены
     * @param shopUnit товар
     */
    public void updateParentCategory(ShopUnit shopUnit) {
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
