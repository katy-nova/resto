package com.katynova.resto.server_side.utility_service;

import com.katynova.resto.server_side.config.WorkTimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class WorkingHoursCounter {

    private final WorkTimeConfig workTimeConfig;

    public LocalDateTime getOpenDateTime(LocalDateTime start) {
        LocalDateTime openDateTime = start.with(workTimeConfig.getOpenTime());
        // обрабатываем случай, если старт перешел через 00:00
        if (openDateTime.isAfter(start)) {
            return openDateTime.minusDays(1);
        }
        return openDateTime;
    }

    public LocalDateTime getCloseDateTime(LocalDateTime end) {
        LocalTime openTime = workTimeConfig.getOpenTime();
        LocalTime closeWeekdayTime = workTimeConfig.getCloseWeekdaysTime();
        LocalTime closeWeekendTime = workTimeConfig.getCloseWeekendTime();
        // если время раньше открытия - значит оно после 00:00
        boolean isAfterMidnight = openTime.isAfter(LocalTime.of(end.getHour(), end.getMinute()));
        // бронь заканчивается до полуночи
        if (!isAfterMidnight) {
            DayOfWeek dayOfWeek = end.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                // если закрываемся после полуночи
                if (openTime.isAfter(closeWeekendTime)) {
                    return LocalDateTime.of(end.getYear(), end.getMonth(), end.getDayOfMonth() + 1, closeWeekendTime.getHour(), 0, 0);
                }
                // если закрываемся до полуночи
                return end.with(closeWeekendTime);
            }
            if (openTime.isAfter(closeWeekdayTime)) {
                return LocalDateTime.of(end.getYear(), end.getMonth(), end.getDayOfMonth() + 1, closeWeekdayTime.getHour(), 0, 0);
            }
            // если закрываемся до полуночи
            return end.with(closeWeekdayTime);
        }
        // бронь заканчивается после полуночи -> ресторан тоже закрывается после полуночи
        DayOfWeek dayOfWeek = end.minusDays(1).getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
            return end.with(closeWeekendTime);
        }
        return end.with(closeWeekdayTime);
    }
}
