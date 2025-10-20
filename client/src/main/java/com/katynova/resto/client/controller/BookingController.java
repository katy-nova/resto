package com.katynova.resto.client.controller;

import com.katynova.resto.client.service.BookingService;
import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.Slot;
import com.katynova.resto.common_dto_library.response.SlotConfirmation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public DeferredResult<ResponseEntity<?>> addRequest(@Valid @RequestBody BookingRequestDto bookingRequestDto) {
        return bookingService.processBookingAsync(bookingRequestDto);
    }

    @PostMapping("/confirm")
    public DeferredResult<ResponseEntity<?>> confirmRequest(@Valid @RequestBody SlotConfirmation slotConfirmation) {
        return bookingService.processConfirmationAsync(slotConfirmation);
    }

    @PostMapping("/test")
    public DeferredResult<ResponseEntity<?>> testRequest() {
        BookingRequestDto bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setPersons(4);
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        bookingRequestDto.setStartTime(LocalDateTime.of(LocalDateTime.now().plusDays(2).toLocalDate(), LocalTime.of(16, 0)));
        return bookingService.processBookingAsync(bookingRequestDto);
    }
}