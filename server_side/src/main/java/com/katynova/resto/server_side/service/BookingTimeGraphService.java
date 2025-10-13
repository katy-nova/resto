package com.katynova.resto.booking.service.time_graph;

import com.katynova.resto.booking.dto.BookingRequestDto;
import com.katynova.resto.booking.service.raw_booking_sql.model.FindResponse;
import com.katynova.resto.booking.dto.response.*;
import com.katynova.resto.booking.exception.ConfirmationException;
import com.katynova.resto.booking.model.Booking;
import com.katynova.resto.booking.model.Status;
import com.katynova.resto.booking.repository.BookingRepository;
import com.katynova.resto.booking.repository.TableRepository;
import com.katynova.resto.booking.service.BookingService;
import com.katynova.resto.booking.service.time_graph.model.AppropriateBookingInfo;
import com.katynova.resto.booking.service.time_graph.model.GraphSlot;
import com.katynova.resto.booking.service.time_graph.model.SuggestBookingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class BookingTimeGraphService implements BookingService {

    private final BookingRepository bookingRepository;
    private final TimeGraphService timeGraphService;
    private final TableRepository tableRepository;
    // здесь храним ссылки на все зарезервированные слоты по correlationId

    @Override
    @Transactional
    public BookingResponse getResponse(BookingRequestDto bookingRequestDto) {
        BookingResponse result;
        try {
            FindResponse<?> response = timeGraphService.findBooking(bookingRequestDto);
            switch (response.getStatus()) {
                case SUCCESS -> result = getSuccessResponse(bookingRequestDto, response);
                case SUGGESTED -> result = sendSuggestResponse(bookingRequestDto, response);
                case WAITLIST -> result = sendWaitlistResponse(bookingRequestDto, response);
                default -> result = sendErrorResponse(bookingRequestDto, "Непредвиденная ошибка");
            }
        } catch (Exception e) {
            result = sendErrorResponse(bookingRequestDto, e.getMessage());
        }
        return result;
    }

    protected BookingResponse sendErrorResponse(BookingRequestDto bookingRequestDto, String message) {
        BookingResponse error = new BookingErrorResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId(), message);
        log.info("Error response: {}", error);
        return error;
    }

    protected BookingResponse sendWaitlistResponse(BookingRequestDto bookingRequestDto, FindResponse<?> response) {
        Random random = new Random();
        int queuePosition = random.nextInt(1000);

        BookingResponse wait = new WaitlistResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId(), queuePosition);
        log.info("Waitlist response: {}", wait);
        return wait;
    }

    protected BookingResponse sendSuggestResponse(BookingRequestDto bookingRequestDto, FindResponse<?> response) {
        // не делаю проверку, тк здесь не может оказаться другого типа
        @SuppressWarnings("unchecked")
        List<SuggestBookingInfo> listOfSuggestions =  (List<SuggestBookingInfo>) response.getList();
        List<Slot> slots = new ArrayList<>();
        for (SuggestBookingInfo suggestBookingInfo : listOfSuggestions) {
            Slot slot = createSlot(bookingRequestDto.getStartTime(), suggestBookingInfo);
            slots.add(slot);
        }
        return new BookingSuggestResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId(),
                slots, LocalDateTime.now().plusMinutes(10));
    }

    private Slot createSlot(LocalDateTime start, SuggestBookingInfo suggestBookingInfo) {
        // обрабатываем возможный переход через 00:00
        // не знаю, как здесь сделать правильно, было бы удобнее не обрабатывать, ведь ключ в мапе останется по прошлому
        //дню, но надо же во внешку отдавать красиво
        LocalDate day = start.toLocalDate();
        LocalDate startDay = day;
        LocalDate endDay = day;
        LocalTime askedTime = start.toLocalTime();
        LocalTime startTime = suggestBookingInfo.getSlots().getFirst().getTime();
        if (askedTime.minusHours(1).isAfter(askedTime)) {
            startDay = day.minusDays(1);
        }
        LocalTime end = suggestBookingInfo.getSlots().getLast().getTime().plusMinutes(30);
        if (end.isBefore(askedTime)) {
            endDay = day.plusDays(1);
        }
        LocalDateTime startDateTime = LocalDateTime.of(startDay, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDay, end);
        // здесь мы вместо букинг id будем использовать номер стола, чтобы потом найти эту бронь
        return new Slot((long) suggestBookingInfo.getTableNumber(), startDateTime, endDateTime);
    }

    @Transactional
    protected BookingResponse getSuccessResponse(BookingRequestDto bookingRequestDto, FindResponse<?> response) {
        @SuppressWarnings("unchecked")
        AppropriateBookingInfo<GraphSlot> info = (AppropriateBookingInfo<GraphSlot>) response.getList().getFirst();
        Booking booking = new Booking();
        booking.setStartTime(bookingRequestDto.getStartTime());
        booking.setEndTime(bookingRequestDto.getStartTime().plus(bookingRequestDto.getDuration()));
        booking.setStatus(Status.CONFIRMED);
        booking.setRestTable(tableRepository.getReferenceById(info.getTableNumber()));
        booking.setGuestId(bookingRequestDto.getGuestId());
        booking.setPersons(bookingRequestDto.getPersons());
        if (bookingRequestDto.getNotes() != null && !bookingRequestDto.getNotes().isEmpty()) {
            booking.setNotes(bookingRequestDto.getNotes());
        }
        // сохраняем бронь в реп
        bookingRepository.save(booking);
        // теперь у брони есть айди и мы ставим их id слотам
        // не вижу смысла передавать это в тайм граф, слоты уже зарезервированы, просто меняем им id
        info.getSlots().forEach(slot -> slot.book(booking.getId()));
        return new BookingSuccessResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId());
    }

    @Override
    public void removeSuggestedBookings(BookingResponse response) {
        timeGraphService.removeSuggestedBookings(response);
    }

    @Override
    @Transactional
    public BookingResponse confirmSlot(SlotConfirmation slot, BookingResponse response, BookingRequestDto bookingRequestDto) {
        int tableNum = slot.getSlot().getBookingId().intValue();
        if (!timeGraphService.containsCorrelationId(bookingRequestDto.getCorrelationId())) {
            log.warn("Зарезервированные слоты для бронирования {} не найдены", response.getCorrelationId());
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(),
                    "Зарезервированные слоты для бронирования не найдены");
        }
        if (!timeGraphService.containsSlotsForTable(bookingRequestDto.getCorrelationId(), tableNum)) {
            log.warn("Слот для стола {} не найден", tableNum);
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(),
                    "Слот для указанного стола не найден");
        }
        BookingSuggestResponse suggestResponse = (BookingSuggestResponse) response;
        if (!suggestResponse.getSlots().contains(slot.getSlot())) {
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(),
                    "Слот для указанного времени не найден");
        }
        try {
            Booking booking = new Booking();
            booking.setGuestId(bookingRequestDto.getGuestId());
            booking.setPersons(bookingRequestDto.getPersons());
            booking.setStartTime(slot.getSlot().getStartTime());
            booking.setEndTime(slot.getSlot().getEndTime());
            booking.setStatus(Status.CONFIRMED);
            if (bookingRequestDto.getNotes() != null && !bookingRequestDto.getNotes().isEmpty()) {
                booking.setNotes(bookingRequestDto.getNotes());
            }
            booking.setRestTable(tableRepository.findByTableNumber(tableNum).orElseThrow());
            bookingRepository.save(booking);
            timeGraphService.confirmSlot(suggestResponse, tableNum, booking.getId());
        } catch (NoSuchElementException e) {
            log.warn("Стол для бронирования {} не найдены", response.getCorrelationId());
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(), "Резерв для бронирования не найден");
        } catch (ConfirmationException e) {
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(), e.getMessage());
        } catch (Exception e) {
            log.warn("Непредвиденная ошибка для брони: {}", response.getCorrelationId());
            return new BookingErrorResponse(response.getCorrelationId(), response.getRequestId(), "Непредвиденная ошибка");
        }
        return new BookingSuccessResponse(response.getCorrelationId(), response.getRequestId());
    }
}
