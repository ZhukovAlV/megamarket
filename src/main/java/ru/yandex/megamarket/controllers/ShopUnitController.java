package ru.yandex.megamarket.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitStatisticResponse;
import ru.yandex.megamarket.services.ShopUnitService;
import ru.yandex.megamarket.error.Error;

import java.util.List;


@Controller
public class ShopUnitController {

    private final ShopUnitService shopUnitService;

    @Autowired
    public ShopUnitController(ShopUnitService shopUnitService) {
        this.shopUnitService = shopUnitService;
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

    @PostMapping(value = "imports/", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> importItems(@RequestBody ShopUnitImportRequest shopUnitImportRequest) {
        // TODO доделать shopUnitService.importItems
        boolean imported = shopUnitService.importItems(shopUnitImportRequest);
        return imported ?
                new ResponseEntity<>(HttpStatus.OK) :
                new ResponseEntity<>("Validation Failed", HttpStatus.BAD_REQUEST);
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
