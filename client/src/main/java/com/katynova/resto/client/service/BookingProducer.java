package com.katynova.resto.client.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.SlotConfirmation;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class BookingProducer {

    private final ReplyingKafkaTemplate<String, BookingRequestDto, BookingResponse> kafkaTemplate;
    private final ReplyingKafkaTemplate<String, SlotConfirmation, BookingResponse> slotConfirmationKafkaTemplate;

    public CompletableFuture<BookingResponse> createBooking(BookingRequestDto bookingRequestDto) {
        try {
            String correlationId = UUID.randomUUID().toString();
            bookingRequestDto.setCorrelationId(correlationId);
            ProducerRecord<String, BookingRequestDto> producerRecord =
                    new ProducerRecord<>("request_topic", correlationId, bookingRequestDto);

            RequestReplyFuture<String, BookingRequestDto, BookingResponse> future =
                    kafkaTemplate.sendAndReceive(producerRecord);

            return future.toCompletableFuture()
                    .thenApply(ConsumerRecord::value)
                    .orTimeout(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<BookingResponse> confirmBooking(SlotConfirmation slotConfirmation) {
        try {
            ProducerRecord<String, SlotConfirmation> producerRecord =
                    new ProducerRecord<>("confirmation_topic", slotConfirmation.getCorrelationId(), slotConfirmation);

            RequestReplyFuture<String, SlotConfirmation, BookingResponse> future =
                    slotConfirmationKafkaTemplate.sendAndReceive(producerRecord);

            return future.toCompletableFuture()
                    .thenApply(ConsumerRecord::value)
                    .orTimeout(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
