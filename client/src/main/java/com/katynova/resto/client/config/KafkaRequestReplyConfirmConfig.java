package com.katynova.resto.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.SlotConfirmation;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaRequestReplyConfirmConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        return mapper;
    }

    @Bean
    public ProducerFactory<String, SlotConfirmation> slotConfirmationProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, BookingResponse> slotConfirmationReplyConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "slot-confirmation-reply-group");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.katynova.resto.common_dto_library,java.util,java.lang");

        JsonDeserializer<BookingResponse> valueDeserializer = new JsonDeserializer<>(BookingResponse.class, objectMapper());
        ErrorHandlingDeserializer<BookingResponse> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, BookingResponse> slotConfirmationReplyContainer() {
        ConcurrentKafkaListenerContainerFactory<String, BookingResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(slotConfirmationReplyConsumerFactory());

        ConcurrentMessageListenerContainer<String, BookingResponse> container =
                factory.createContainer("slot-confirmation-response-topic");
        container.getContainerProperties().setGroupId("slot-confirmation-reply-group");
        return container;
    }

    @Bean
    public ReplyingKafkaTemplate<String, SlotConfirmation, BookingResponse> slotConfirmationReplyingKafkaTemplate() {
        ReplyingKafkaTemplate<String, SlotConfirmation, BookingResponse> template =
                new ReplyingKafkaTemplate<>(slotConfirmationProducerFactory(), slotConfirmationReplyContainer());
        template.setDefaultReplyTimeout(Duration.ofSeconds(30));
        return template;
    }
}

