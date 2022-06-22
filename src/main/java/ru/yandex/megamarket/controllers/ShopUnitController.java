package ru.yandex.megamarket.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
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
     * @return HttpStatus OK
     */
    @PostMapping(value = "/imports", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> importItems(ShopUnitImportRequest shopUnitImportRequest) {
        shopUnitService.importShopUnitItems(shopUnitImportRequest);
        return ResponseEntity.ok().build();
    }

/*    @GetMapping(value = "nodes/{id}", produces = "application/json")
    public ResponseEntity<?> getItem(@PathVariable(name = "id") String id) {
        if (id == null || id.equals("") || id.contains(" ")) {
            return new ResponseEntity<>(new Error(404, "Validation Failed"), HttpStatus.BAD_REQUEST);
        }
        // TODO доделать shopUnitService.getInfoOfItemAndItsChildrenById
        final ShopUnit shopUnit = shopUnitService.getInfoOfItemAndItsChildrenById(id);
        return shopUnit != null ?
                new ResponseEntity<>(shopUnit, HttpStatus.OK) :
                new ResponseEntity<>(new Error(404, "Item not found"), HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(value = "delete/{id}", produces = "application/json")
    public ResponseEntity<?> deleteItem(@PathVariable(name = "id") String id) {
        if (id==null || id.equals("") || id.contains(" ")) {
            return new ResponseEntity<>(new Error(404, "Validation Failed"), HttpStatus.BAD_REQUEST);
        }
        // TODO доделать shopUnitService.deleteItemByIdAndChildren
        boolean deleted = shopUnitService.deleteItemByIdAndChildren(id);

        return deleted ?
                new ResponseEntity<>(HttpStatus.OK) :
                new ResponseEntity<>(new Error(404, "Item not found"), HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "sales/{date}", produces = "application/json")
    public ResponseEntity<?> salesOfPast24Hours(@PathVariable(name = "date") String date) {
        // TODO доделать shopUnitService.getSalesOfPast24Hours
        final ShopUnitStatisticResponse response = shopUnitService.getSalesOfPast24Hours(date);
        return response == null ?
                new ResponseEntity<>("no",HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(response, HttpStatus.OK);
    }*/


    /**
     * Вывод списка ShopUnit
     * @return список ShopUnit
     */
    @GetMapping(value = "/nodes")
    @ResponseBody
    public  List<ShopUnit> getAllShopUnits() {
        return shopUnitService.getShopUnits();
    }

    // Сохранить в базе для теста
/*    @GetMapping("/save")
    public ModelAndView saveAllShopUnits() {
        List<ShopUnit> shopUnitList = shopUnitService.getShopUnits();
        for (ShopUnit shopUnit : shopUnitList) {
            shopUnitService.saveShopUnit(shopUnit);
        }
        ModelAndView modelAndView = new ModelAndView("shopUnitList");
        return modelAndView;
    }*/

}
