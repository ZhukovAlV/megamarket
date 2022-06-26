package ru.yandex.megamarket.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.megamarket.exception.ValidationFailedException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@Slf4j
public class ParserService {

    private static final int LENGTH_UUID = 36;
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS[X]";

    /**
     * Проверка даты на ISO 8601
     * @param date дата
     * @return дата в формате LocalDateTime
     */
    public OffsetDateTime getIsoDate(String date) {
        try {
            log.info("Контвертация даты из строки:" + date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
            log.info("Контвертация прошла успешно");
            return OffsetDateTime.parse(date, formatter);
        } catch (DateTimeParseException e) {
            log.error("Дата не соответствует формату");
            throw new ValidationFailedException();
        }
    }

    /**
     * Преобразование String в UUID
     * @param id идентификатор в String
     * @return идентификатор UUID
     */
    public UUID stringToUUID(String id) {
        if (id == null) {
            log.error("id отсутствует");
            return null;
        } else if (id.length() != LENGTH_UUID) {
            log.error("id не соответствует формату UUID");
            throw new ValidationFailedException();
        } else {
            log.info("id соответствует формату UUID");
            return UUID.fromString(id);
        }
    }
}
