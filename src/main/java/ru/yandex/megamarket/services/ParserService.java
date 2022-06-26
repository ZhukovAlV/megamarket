package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.megamarket.exception.ValidationFailedException;
import ru.yandex.megamarket.model.ShopUnitImport;
import ru.yandex.megamarket.model.ShopUnitImportRequest;
import ru.yandex.megamarket.model.ShopUnitType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ParserService {

    private static final int LENGTH_UUID = 36;

    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]";

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
     * Преобразование String в UUID
     *
     * @param id идентификатор в String
     * @return идентификатор UUID
     */
    public UUID getUUIDFromString(String id) {
        if (id == null || id.equals("") || id.contains(" ")) {
            log.warn("Невалидная схема документа или входные данные не верны (id)");
            throw new ValidationFailedException();
        } else if (id.length() != 36) {
            log.warn("Невалидная схема документа или входные данные не верны (id)");
            throw new ValidationFailedException();
        }

        try {
            UUID idUUID = UUID.fromString(id);
            log.info("id соответствует формату UUID");
            return idUUID;
        } catch (RuntimeException e) {
            log.warn("Невалидная схема документа или входные данные не верны (id)");
            throw new ValidationFailedException();
        }
    }

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
        } else {
            throw new ValidationFailedException();
        }

    }

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     * @return дата в формате LocalDateTime
     */
    public OffsetDateTime getIsoDate(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
            return OffsetDateTime.parse(date, formatter);
        } catch (DateTimeParseException e) {
            throw new ValidationFailedException();
        }
    }

    /**
     * Преобразование String в UUID
     * @param id идентификатор в String
     * @return идентификатор UUID
     */
    public UUID stringToUUID(String id) {
        if (id == null) return null;
        else if (id.length() != LENGTH_UUID) throw new ValidationFailedException();
        else return UUID.fromString(id);
    }
}
