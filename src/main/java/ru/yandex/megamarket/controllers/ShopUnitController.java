package ru.yandex.megamarket.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ru.yandex.megamarket.model.ShopUnit;
import ru.yandex.megamarket.services.ShopUnitService;
import java.util.List;


@Controller
public class ShopUnitController {

    private final ShopUnitService shopUnitService;

    @Autowired
    public ShopUnitController(ShopUnitService shopUnitService) {
        this.shopUnitService = shopUnitService;
    }

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
