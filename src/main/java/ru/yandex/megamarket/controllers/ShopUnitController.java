package ru.yandex.megamarket.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.megamarket.exception.ItemNotFoundException;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.services.ShopUnitService;

import java.util.List;


@RestController
public class ShopUnitController {

    private final ShopUnitService shopUnitService;

    @Autowired
    public ShopUnitController(ShopUnitService shopUnitService) {
        this.shopUnitService = shopUnitService;
    }

    /**
     * Импортирует новые товары и/или категории.
     * Товары/категории импортированные повторно обновляют текущие.
     * Изменение типа элемента с товара на категорию или с категории на товар не допускается.
     * Порядок элементов в запросе является произвольным.
     * @param shopUnitImportRequest Запрос с данными
     * @return ResponseEntity со статусом ОК
     */
    @PostMapping(value = "/imports")
    public ResponseEntity<?> importItems(@RequestBody ShopUnitImportRequest shopUnitImportRequest) {
        shopUnitService.importShopUnitItems(shopUnitImportRequest);
        return ResponseEntity.ok().build();
    }

    /**
     * Удаление товара или категории из базы
     * @param id идентификатор товара или категории
     * @return ResponseEntity со статусом ОК
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteShopUnitById(@PathVariable String id) {
        shopUnitService.deleteShopUnitById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Вывод информации по товару или категории
     * @param id идентификатор товара или категории
     * @return ResponseEntity со статусом ОК
     */
    @GetMapping(value = "nodes/{id}")
    public ResponseEntity<?> getShopUnit(@PathVariable("id") String id) {
        ShopUnit shopUnit = shopUnitService.getShopUnitById(id).orElseThrow(ItemNotFoundException::new);
        return new ResponseEntity<>(shopUnit, HttpStatus.OK);
    }

/*    @GetMapping("/sales")
    public ResponseEntity<?> getUnitsByChangePriceLast24Hours(
            @RequestParam(name = "date") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") String strDateTime) {
        ShopUnitStatisticResponse statistic = shopUnitService.getSalesStatisticFor24Hour(strDateTime);
        return new ResponseEntity<>(statistic, HttpStatus.OK);
    }*/

    /**
     * Вывод списка ShopUnit
     * @return список ShopUnit
     */
    @GetMapping(value = "/nodes")
    public  List<ShopUnit> getAllShopUnits() {
        return shopUnitService.getShopUnits();
    }

/*    @GetMapping(value = "/test")
    public ShopUnitImportRequest testShopUnits() {
        List<ShopUnitImport> items = new ArrayList<>();
        items.add(
                new ShopUnitImport("3fa85f64-5717-4562-b3fc-2c963f66a111", "Товар1",
                        "3fa85f64-5717-4562-b3fc-2c963f66a222", ShopUnitType.OFFER,
                        100L));
        items.add(
                new ShopUnitImport("3fa85f64-5717-4562-b3fc-2c963f66a222", "Категория1",
                        null, ShopUnitType.CATEGORY,
                        null));

        return new ShopUnitImportRequest(items, "2022-05-26T21:12:01.000Z");
    }*/

    // Сохранить в базе для теста
/*    @GetMapping("/test")
    public void testShopUnits() {
        ShopUnit shopUnitCat = new ShopUnit.Builder()
                .withId("3fa85f64-5717-4562-b3fc-2c963f66a111")
                .withName("Категория")
                .withDate(LocalDateTime.now())
                .withType(ShopUnitType.CATEGORY)
                .build();

        ShopUnit shopUnitOff = new ShopUnit.Builder()
                .withId("3fa85f64-5717-4562-b3fc-2c963f66a222")
                .withName("Категория")
                .withDate(LocalDateTime.now())
                .withParentId("3fa85f64-5717-4562-b3fc-2c963f66a111")
                .withType(ShopUnitType.OFFER)
                .withPrice(100L)
                .build();

        shopUnitService.saveShopUnit(shopUnitCat);
        shopUnitService.saveShopUnit(shopUnitOff);
    }*/
}
