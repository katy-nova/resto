package com.katynova.resto.server_side.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, BookingResponse> kafkaTemplate;
    private final BookingService bookingService;


    @KafkaListener(topics = "request_topic", containerFactory = "factory")
    public void listenRequest(ConsumerRecord<String, BookingRequestDto> record) {
        BookingRequestDto request = record.value();
        log.info("Received request: {}", request);

        Header correlationHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
        byte[] correlationId = correlationHeader != null ? correlationHeader.value() : record.key().getBytes(StandardCharsets.UTF_8);
        BookingResponse result = bookingService.getResponse(request);
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
        BookingResponse result = bookingService.confirmSlot(confirmation);
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
