package com.katynova.resto.server_side.service;

import com.katynova.resto.server_side.model.GraphSlot;
import com.katynova.resto.server_side.model.entity.RestTable;
import com.katynova.resto.server_side.utility_service.WorkingHoursCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphCreator {

    private final WorkingHoursCounter workingHoursCounter;

    // создает день с необходимым количеством слотов
    ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>> createDay(LocalDate date, List<RestTable> tables) {
        LocalDateTime openTime = workingHoursCounter.getOpenDateTime(LocalDateTime.of(date, LocalTime.of(14, 0)));
        LocalDateTime closeTime = workingHoursCounter.getCloseDateTime(LocalDateTime.of(date, LocalTime.of(14, 0)));
        long numberOfSlots = Duration.between(openTime, closeTime).toMinutes() / 30;
        LocalTime startTime = openTime.toLocalTime();
        ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>> day = tables.stream()
                .collect(Collectors.groupingByConcurrent(
                        RestTable::getCapacity,
                        Collectors.toConcurrentMap(
                                RestTable::getTableNumber,
                                table -> createTable(startTime, numberOfSlots)
                        )
                ));
        return day;
    }

    private List<GraphSlot> createTable(LocalTime startTime, long numberOfSlots) {
        return IntStream.range(0, (int) numberOfSlots)
                .mapToObj(i -> new GraphSlot(
                        startTime.plusMinutes(30L * i),
                        new AtomicBoolean(true),  // доступен
                        new AtomicReference<Long>(null)  // без брони
                ))
                .toList();
        // неизменяемый список, тк мы не будем добавлять/удалять слоты
    }
}
