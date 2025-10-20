package com.katynova.resto.server_side.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.*;
import com.katynova.resto.server_side.model.FindResponse;
import com.katynova.resto.server_side.model.entity.Booking;
import com.katynova.resto.server_side.model.info.AppropriateBookingInfo;
import com.katynova.resto.server_side.model.info.SuggestBookingInfo;
import com.katynova.resto.server_side.model.status.Status;
import com.katynova.resto.server_side.repository.BookingRepository;
import com.katynova.resto.server_side.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

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

    public BookingResponse sendSuggestResponse(BookingRequestDto bookingRequestDto, FindResponse<?> response) {
        // не делаю проверку, тк здесь не может оказаться другого типа
        @SuppressWarnings("unchecked")
        List<SuggestBookingInfo> listOfSuggestions = (List<SuggestBookingInfo>) response.getList();
        List<Slot> slots = new ArrayList<>();
        LocalDateTime expired = LocalDateTime.now().plusMinutes(10);
        for (SuggestBookingInfo suggestBookingInfo : listOfSuggestions) {
            Booking booking = createBooking(bookingRequestDto, suggestBookingInfo.getTableNumber(), Status.PENDING);
            booking.setExpired(expired);
            bookingRepository.save(booking);
            suggestBookingInfo.getSlots().forEach(graphSlot -> graphSlot.book(booking.getId()));
            Slot slot = createSlot(bookingRequestDto.getStartTime(), suggestBookingInfo, booking.getId());
            slots.add(slot);
        }
        return new BookingSuggestResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId(),
                slots, expired);
    }

    private Booking createBooking(BookingRequestDto bookingRequestDto, int tableNumber, Status status) {
        Booking booking = new Booking();
        booking.setStartTime(bookingRequestDto.getStartTime());
        booking.setEndTime(bookingRequestDto.getStartTime().plus(bookingRequestDto.getDuration()));
        booking.setStatus(status);
        booking.setRestTable(tableRepository.getReferenceById(tableNumber));
        booking.setGuestId(bookingRequestDto.getGuestId());
        booking.setPersons(bookingRequestDto.getPersons());
        if (bookingRequestDto.getNotes() != null && !bookingRequestDto.getNotes().isEmpty()) {
            booking.setNotes(bookingRequestDto.getNotes());
        }
        return booking;
    }

    private Slot createSlot(LocalDateTime start, SuggestBookingInfo suggestBookingInfo, Long bookingId) {
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
        return new Slot(bookingId, startDateTime, endDateTime);
    }

    @Transactional
    protected BookingResponse getSuccessResponse(BookingRequestDto bookingRequestDto, FindResponse<?> response) {
        @SuppressWarnings("unchecked")
        AppropriateBookingInfo info = (AppropriateBookingInfo) response.getList().getFirst();
        Booking booking = createBooking(bookingRequestDto, info.getTableNumber(), Status.CONFIRMED);
        // сохраняем бронь в реп
        bookingRepository.save(booking);
        // теперь у брони есть айди и мы ставим их id слотам
        // не вижу смысла передавать это в тайм граф, слоты уже зарезервированы, просто меняем им id
        info.getSlots().forEach(slot -> slot.book(booking.getId()));
        return new BookingSuccessResponse(bookingRequestDto.getCorrelationId(), bookingRequestDto.getRequestId());
    }

    @Override
    @Transactional
    public BookingResponse confirmSlot(SlotConfirmation slot) {
        try {
            Booking booking = bookingRepository.findById(slot.getConfirmSlotId())
                    .orElseThrow(NoSuchElementException::new);
            booking.setStatus(Status.CONFIRMED);
            if (!slot.getRejectedSlotIds().isEmpty()) {
                bookingRepository.deleteAllById(slot.getRejectedSlotIds());
                timeGraphService.unreserveSlotsByBookingId(slot.getRejectedSlotIds(), booking.getStartTime());
            }
            return new BookingSuccessResponse(slot.getCorrelationId(), slot.getRequestId());
        } catch (NoSuchElementException e) {
            return new BookingErrorResponse(slot.getCorrelationId(), slot.getRequestId(),
                    "Время подтверждения бронирования истекло");
        }
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanUpExpiredBookings() {
        List<Long> expiredIds = bookingRepository.deleteExpiredBookingsAndReturnIds();
        if (!expiredIds.isEmpty()) {
            timeGraphService.unreserveSlotsByBookingId(expiredIds, LocalDateTime.now());
        }
    }

}
