package com.katynova.resto.server_side.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.BookingSuccessResponse;
import com.katynova.resto.common_dto_library.response.SlotConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final KafkaTemplate<String, BookingResponse> kafkaTemplate;

    @KafkaListener(topics = "request_topic", containerFactory = "factory")
    public void listenRequest(ConsumerRecord<String, BookingRequestDto> record) {
        BookingRequestDto request = record.value();
        log.info("Received request: {}", request);

        Header correlationHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
        byte[] correlationId = correlationHeader != null ? correlationHeader.value() : record.key().getBytes(StandardCharsets.UTF_8);
        BookingResponse result = new BookingSuccessResponse(request.getCorrelationId(), request.getRequestId());
        // Отправляем ответ с правильными заголовками
        Message<BookingResponse> responseMessage = MessageBuilder
                .withPayload(result)
                .setHeader(KafkaHeaders.TOPIC, "response_topic")
                .setHeader(KafkaHeaders.KEY, record.key())
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationId)
                .build();

        kafkaTemplate.send(responseMessage);
    }

    @KafkaListener(topics = "confirmation_topic", containerFactory = "slotFactory")
    public void confirm(ConsumerRecord<String, SlotConfirmation> record) {
        SlotConfirmation confirmation = record.value();
        log.info("Received confirmation: {}", confirmation);
        Header correlationHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
        byte[] correlationId = correlationHeader != null ? correlationHeader.value() : record.key().getBytes(StandardCharsets.UTF_8);
        BookingResponse result = new BookingSuccessResponse(confirmation.getCorrelationId(), 0L);
        // Отправляем ответ с правильными заголовками
        Message<BookingResponse> responseMessage = MessageBuilder
                .withPayload(result)
                .setHeader(KafkaHeaders.TOPIC, "slot-confirmation-response-topic")
                .setHeader(KafkaHeaders.KEY, record.key())
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationId)
                .build();

        kafkaTemplate.send(responseMessage);
    }
}
