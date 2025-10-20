package com.katynova.resto.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Random;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingProducer bookingProducer;

     /* DeferredResult — это механизм в Spring MVC для асинхронной обработки HTTP-запросов без блокировки серверных потоков.
    Он позволяет отложить отправку ответа клиенту до тех пор, пока результат не будет готов
    (например, приходит ответ из Kafka, базы данных или другого микросервиса).*/

    public DeferredResult<ResponseEntity<?>> processBookingAsync(BookingRequestDto bookingRequestDto) {
        Random random = new Random();
        bookingRequestDto.setRequestId(random.nextLong(1000L));
        bookingRequestDto.setGuestId(random.nextLong(1000L));
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(35_000L);

        deferredResult.onTimeout(() -> {
            log.warn("Overall timeout for requestId: {}", bookingRequestDto.getRequestId());
            deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Превышено общее время обработки"));
        });

        bookingProducer.createBooking(bookingRequestDto)
                .whenComplete((bookingResponse, ex) -> {
                    if (ex != null) {
                        if (ex instanceof TimeoutException) {
                            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Превышено время ожидания"));
                        } else {
                            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Непредвиденная ошибка"));
                        }
                    } else {
                        deferredResult.setResult(toResponseEntity(bookingResponse));
                    }
                });

        return deferredResult;
    }

    //TODO красиво упаковать для клиента
    private ResponseEntity<?> toResponseEntity(BookingResponse bookingResponse) {
        if (bookingResponse instanceof BookingSuccessResponse) {
            return ResponseEntity.ok(bookingResponse);
        } else if (bookingResponse instanceof BookingSuggestResponse || bookingResponse instanceof WaitlistResponse) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(bookingResponse);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(bookingResponse);
        }
    }

    public DeferredResult<ResponseEntity<?>> processConfirmationAsync(SlotConfirmation slotConfirmation) {
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(30_000L);
        deferredResult.onTimeout(() -> {
            log.warn("Overall timeout");
            deferredResult.setResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Превышено общее время обработки"));
        });

        bookingProducer.confirmBooking(slotConfirmation)
                .whenComplete((bookingResponse, ex) -> {
                    if (ex != null) {
                        if (ex instanceof TimeoutException) {
                            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Превышено время ожидания"));
                        } else {
                            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Непредвиденная ошибка"));
                        }
                    } else {
                        deferredResult.setResult(toResponseEntity(bookingResponse));
                    }
                });
        return deferredResult;
    }
}
