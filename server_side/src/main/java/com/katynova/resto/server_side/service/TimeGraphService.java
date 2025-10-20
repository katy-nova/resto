package com.katynova.resto.server_side.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.BookingSuggestResponse;
import com.katynova.resto.server_side.config.TimeGraphRefreshEvent;
import com.katynova.resto.server_side.exception.ConsistencyException;
import com.katynova.resto.server_side.model.FindResponse;
import com.katynova.resto.server_side.model.GraphSlot;
import com.katynova.resto.server_side.model.entity.Booking;
import com.katynova.resto.server_side.model.entity.RestTable;
import com.katynova.resto.server_side.model.info.AppropriateBookingInfo;
import com.katynova.resto.server_side.model.info.SuggestBookingInfo;
import com.katynova.resto.server_side.model.status.ResponseStatus;
import com.katynova.resto.server_side.repository.BookingRepository;
import com.katynova.resto.server_side.repository.TableRepository;
import com.katynova.resto.server_side.utility_service.CapacityService;
import com.katynova.resto.server_side.utility_service.WorkingHoursCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeGraphService implements ApplicationListener<TimeGraphRefreshEvent> {
    private final BookingRepository bookingRepository;
    private final TableRepository tableRepository;
    private final GraphCreator graphCreator;
    private final WorkingHoursCounter workingHoursCounter;
    private final CapacityService capacityService;

    private final ConcurrentMap<LocalDate, ConcurrentMap
            <Integer, ConcurrentMap
                    <Integer, List<GraphSlot>>>> timeGraph = new ConcurrentHashMap<>();

    private final ConcurrentMap<LocalDate, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ConcurrentMap<LocalDate, ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>>> getTimeGraph() {
        return timeGraph;
    }

    @Transactional(readOnly = true)
    public void fillIn() {
        locks.clear();
        timeGraph.clear();
        // достаем 1 запросом и столы, и бронирования
        List<RestTable> tablesWithBookings = tableRepository.findAllWithBookings();
        // предполагаем высоконагруженные системы, сделаем parallelStream
        bookingRepository.findAll().parallelStream().forEach(booking -> {
            addBookingFromDB(booking, tablesWithBookings);
        });
        log.info("Filling up time-graph");
    }

    // добавляем бронирования из базы
    private void addBookingFromDB(Booking booking, List<RestTable> tables) {
        LocalDate day = booking.getStartTime().toLocalDate();
        LocalTime startTime = booking.getStartTime().toLocalTime();
        LocalTime endTime = booking.getEndTime().toLocalTime();
        // предполагается, что данные в бд полностью согласованы
        ReentrantLock lock = locks.computeIfAbsent(day, k -> new ReentrantLock());
        lock.lock();
        try {
            // Атомарно создаём день если его нет
            ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>> daySlotsMap =
                    timeGraph.computeIfAbsent(day, d -> graphCreator.createDay(d, tables));
            int capacity = booking.getRestTable().getCapacity();
            int tableNumber = booking.getTableNumber();
            daySlotsMap.get(capacity).get(tableNumber).stream()
                    .filter(slot -> isSlotInTimeRange(slot, startTime, endTime))
                    .forEach(slot -> {
                        if (!slot.isAvailable()) {
                            String errorMessage = String.format("Слот для стола %s уже забронирован на время %s",
                                    tableNumber, slot.getTime().toString());
                            log.error(errorMessage);
                            throw new ConsistencyException(errorMessage);
                            // предполагается, что данные в репозитории согласованы, поэтому здесь выкинем исключение
                        }
                        slot.book(booking.getId());
                    });
        } finally {
            lock.unlock();
        }
    }

    // ищем нужные слоты и резервируем их в графе
    public FindResponse<?> findBooking(BookingRequestDto bookingRequestDto) {
        LocalDate day = workingHoursCounter.getOpenDateTime(bookingRequestDto.getStartTime()).toLocalDate();
        LocalTime startTime = bookingRequestDto.getStartTime().toLocalTime();
        LocalTime endTime = startTime.plus(bookingRequestDto.getDuration());
        int capacity = capacityService.getCapacity(bookingRequestDto.getPersons());
        ReentrantLock lock = locks.computeIfAbsent(day, k -> new ReentrantLock());
        try {
            if (!lock.tryLock(1, TimeUnit.SECONDS)) {
                return new FindResponse<>(List.of("Ошибка при запросе на бронирование. Повторите попытку позже"), ResponseStatus.ERROR);
            }
            try {
                boolean isDayExists = timeGraph.containsKey(day);
                if (!isDayExists) {
                    return addFirstBookingInDay(day, startTime, endTime, capacity);
                }
                FindResponse<AppropriateBookingInfo> maybeBooking = findAppropriateTableForBooking(day, startTime, endTime, capacity);
                if (maybeBooking != null) {
                    return maybeBooking;
                }
                int increasedCapacity = capacity + 2;
                boolean hasIncreasedCapacity = capacityService.getCapacities().contains(increasedCapacity);
                if (hasIncreasedCapacity) {
                    maybeBooking = findAppropriateTableForBooking(day, startTime, endTime, increasedCapacity);
                    if (maybeBooking != null) {
                        return maybeBooking;
                    }
                }
                FindResponse<SuggestBookingInfo> suggests = findSuggestions(day, startTime, endTime, capacity,
                        bookingRequestDto.getCorrelationId());
                if (suggests != null) {
                    return suggests;
                }
                if (hasIncreasedCapacity) {
                    FindResponse<SuggestBookingInfo> suggest = findSuggestions(day, startTime, endTime,
                            increasedCapacity, bookingRequestDto.getCorrelationId());
                    if (suggest != null) {
                        return suggest;
                    }
                }
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            log.error("Поток был прерван при поиске бронирования: {}", e.getMessage());
            return new FindResponse<>(
                    List.of("Системная ошибка. Пожалуйста, попробуйте позже"),
                    ResponseStatus.ERROR
            );
        }
        return new FindResponse<>(new ArrayList<>(), ResponseStatus.WAITLIST);
    }

    private FindResponse<SuggestBookingInfo> findSuggestions(LocalDate day, LocalTime startTime, LocalTime endTime,
                                                             int capacity, String correlationId) {
        var decreasedStartTime = decreaseStartTime(startTime, day);
        LocalTime increasedEndTime = increaseEndTime(endTime, day);
        List<SuggestBookingInfo> suggests = new ArrayList<>();
        ConcurrentHashMap<Integer, List<GraphSlot>> slotsMap = new ConcurrentHashMap<>();
        ConcurrentMap<Integer, List<GraphSlot>> allTablesWithCapacity = timeGraph.get(day).get(capacity)
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().stream()
                        .filter(slot -> isSlotInTimeRange(slot, decreasedStartTime, increasedEndTime))
                        .toList()))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
        long neededSlots = getNeededSlots(startTime, endTime);
        for (Map.Entry<Integer, List<GraphSlot>> entry : allTablesWithCapacity.entrySet()) {
            SuggestBookingInfo maybeSuggest = getSuggestions(entry.getKey(), entry.getValue(), neededSlots);
            if (maybeSuggest != null && !suggests.contains(maybeSuggest)) {
                suggests.add(maybeSuggest);
                // добавляем зарезервированные слоты в мапу
                slotsMap.put(entry.getKey(), maybeSuggest.getSlots());
            }
            // нет смысла искать больше 4 предложений
            if (suggests.size() >= 4) {
                break;
            }
        }
        if (!suggests.isEmpty()) {
            return new FindResponse<>(suggests, ResponseStatus.SUGGESTED);
        }
        return null;
    }

    // здесь мы избегаем лишних сохранений в базу и резерв отмечаем только в графе
    private SuggestBookingInfo getSuggestions(int tableNumber, List<GraphSlot> slots, long neededSlots) {
        List<GraphSlot> suggestedSlots = new ArrayList<>();
        int counter = 0;
        for (GraphSlot slot : slots) {
            if (slot.isAvailable()) {
                counter++;
                suggestedSlots.add(slot);
                if (counter == neededSlots) {
                    // тк слоты - это объекты, то они зарезервируются и в графе
                    suggestedSlots.forEach(GraphSlot::reserve);
                    return new SuggestBookingInfo(tableNumber, suggestedSlots);
                }
            } else {
                if (suggestedSlots.size() >= 4) {
                    suggestedSlots.forEach(GraphSlot::reserve);
                    return new SuggestBookingInfo(tableNumber, suggestedSlots);
                } else {
                    counter = 0;
                    suggestedSlots.clear();
                }
            }
        }
        return null;
    }

    private long getNeededSlots(LocalTime startTime, LocalTime endTime) {
        if (endTime.isAfter(startTime)) {
            return Duration.between(startTime, endTime).toMinutes() / 30;
        }
        return (Duration.between(startTime, LocalTime.of(23, 59)).plusMinutes(1).toMinutes() +
                Duration.between(LocalTime.MIDNIGHT, endTime).toMinutes()) / 30;

    }

    private LocalTime decreaseStartTime(LocalTime startTime, LocalDate day) {
        if (!startTime.minusHours(1).isBefore(workingHoursCounter.getOpenDateTime(LocalDateTime.of(day, startTime)).toLocalTime())) {
            startTime = startTime.minusHours(1);
        } else {
            startTime = workingHoursCounter.getOpenDateTime(LocalDateTime.of(day, startTime)).toLocalTime();
        }
        return startTime;
    }

    private LocalTime increaseEndTime(LocalTime endTime, LocalDate day) {
        if (!endTime.plusHours(1).isAfter(workingHoursCounter.getCloseDateTime(LocalDateTime.of(day, endTime)).toLocalTime())) {
            endTime = endTime.plusHours(1);
        } else {
            endTime = workingHoursCounter.getCloseDateTime(LocalDateTime.of(day, endTime)).toLocalTime();
        }
        return endTime;
    }

    private FindResponse<AppropriateBookingInfo> insertBookingFromInfo(AppropriateBookingInfo info) {
        return new FindResponse<>(List.of(info), ResponseStatus.SUCCESS);
    }

    private FindResponse<AppropriateBookingInfo> findAppropriateTableForBooking(LocalDate day, LocalTime startTime, LocalTime endTime, int capacity) {
        ConcurrentMap<Integer, List<GraphSlot>> allTablesWithCapacity = timeGraph.get(day).get(capacity);
        // пробуем найти столы, которые полностью удовлетворяют данным бронирования
        List<AppropriateBookingInfo> slotsForBooking = new ArrayList<>();
        for (Map.Entry<Integer, List<GraphSlot>> entry : allTablesWithCapacity.entrySet()) {
            AppropriateBookingInfo info = findAppropriateSlots(entry.getKey(), startTime, endTime, entry.getValue());
            if (info != null) {
                // если нашли идеально подходящий слот, сразу выходим из метода и сохраняемся в базу
                if (info.getSlotsAfter() == 0 && info.getSlotsBefore() == 0) {
                    return insertBookingFromInfo(info);
                }
                slotsForBooking.add(info);
            }
        }
        if (!slotsForBooking.isEmpty()) {
            // ищем слот, стыкующийся с началом или концом
            Optional<AppropriateBookingInfo> slotForBookingCloseToStartOrEnd = slotsForBooking.stream()
                    .filter(info -> info.getSlotsBefore() == 0 || info.getSlotsAfter() == 0)
                    .max(Comparator.comparingInt(info -> info.getSlotsAfter() + info.getSlotsBefore()));
            if (slotForBookingCloseToStartOrEnd.isPresent()) {
                return insertBookingFromInfo(slotForBookingCloseToStartOrEnd.get());
            }
            // упрощенная сортировка - если нет точного стыка, то просто пытаемся найти максимальный зазор
            // можно расписать также подробно, как в прошлой реализации
            AppropriateBookingInfo appropriateBookingInfo = slotsForBooking.stream()
                    .max(Comparator.comparingInt(info -> info.getSlotsAfter() + info.getSlotsBefore()))
                    .get();
            return insertBookingFromInfo(appropriateBookingInfo);
        }
        return null;
    }

    // время начало брони ГАРАНТИРОВАННО совпадает с временем начала одного из слотов!
    private AppropriateBookingInfo findAppropriateSlots(int tableNumber, LocalTime startTime, LocalTime endTime, List<GraphSlot> allSlots) {
        int slotsBefore = 0;
        int slotsAfter = 0;
        List<GraphSlot> slots = new ArrayList<>();
        Iterator<GraphSlot> iterator = allSlots.iterator();
        while (iterator.hasNext()) {
            GraphSlot slot = iterator.next();
            // используем именно equals, чтобы корректно обработать переход через 0
            if (slot.getTime().equals(startTime)) {
                // первый слот подходящий по времени
                if (slot.isAvailable()) {
                    slots.add(slot);
                } else {
                    return null;
                }
                // прерываем эту логику
                break;
            }
            if (slot.isAvailable()) {
                slotsBefore++;
            } else {
                slotsBefore = 0;
            }
        }
        while (iterator.hasNext()) {
            GraphSlot slot = iterator.next();
            if (slot.getTime().equals(endTime)) {
                if (slot.isAvailable()) {
                    slotsAfter++;
                } else {
                    return new AppropriateBookingInfo(tableNumber, slotsBefore, slotsAfter, slots);
                }
                break;
            }
            if (!slot.isAvailable()) {
                // если хотя бы 1 слот в брони недоступен - прерываем метод
                return null;
            }
            slots.add(slot);
        }
        while (iterator.hasNext()) {
            GraphSlot slot = iterator.next();
            if (slot.isAvailable()) {
                slotsAfter++;
            } else {
                return new AppropriateBookingInfo(tableNumber, slotsBefore, slotsAfter, slots);
            }
        }

        return new AppropriateBookingInfo(tableNumber, slotsBefore, slotsAfter, slots);
    }

    protected FindResponse<AppropriateBookingInfo> addFirstBookingInDay(LocalDate day, LocalTime startTime, LocalTime endTime, int capacity) {
        List<RestTable> allTables = tableRepository.findAll();
        ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>> daySlotsMap = graphCreator
                .createDay(day, allTables);
        timeGraph.put(day, daySlotsMap);
        int tableNumber = allTables.stream()
                .filter(table -> table.getCapacity() == capacity)
                .findAny().get().getTableNumber();
        List<GraphSlot> slots = daySlotsMap.get(capacity).get(tableNumber).stream()
                .filter(slot -> isSlotInTimeRange(slot, startTime, endTime))
                .peek(GraphSlot::reserve) // резервируем стол, так как еще не знаем id брони
                .toList();
        AppropriateBookingInfo info = new AppropriateBookingInfo(tableNumber, 0, 0, slots);
        return new FindResponse<>(List.of(info), ResponseStatus.SUCCESS);
    }

    private boolean isSlotInTimeRange(GraphSlot slot, LocalTime startTime, LocalTime endTime) {
        if (endTime.isAfter(startTime)) {
            return !slot.getTime().isBefore(startTime) && slot.getTime().isBefore(endTime);
        }
        return !slot.getTime().isBefore(startTime) && slot.getTime().isBefore(LocalTime.of(23, 59)) ||
                slot.getTime().isBefore(endTime);
    }


    public void unreserveSlotsByBookingId(List<Long> bookingIds, LocalDateTime startTime) {
        LocalDate day = workingHoursCounter.getOpenDateTime(startTime).toLocalDate();
        ReentrantLock lock = locks.computeIfAbsent(day, k -> new ReentrantLock());
        lock.lock();
        try {
            timeGraph.get(day).values().stream()
                    .flatMap(map -> map.values().stream())
                    .flatMap(Collection::stream)
                    .filter(graphSlot -> bookingIds.contains(graphSlot.bookingId().get()))
                    .forEach(GraphSlot::unreserve);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void onApplicationEvent(TimeGraphRefreshEvent event) {
        this.fillIn();
    }
}
