package com.katynova.resto.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KafkaRequestReplyConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, BookingRequestDto> requestProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, BookingResponse> replyConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "reply-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.katynova.resto.common_dto_library,java.util,java.lang");

        JsonDeserializer<BookingResponse> valueDeserializer = new JsonDeserializer<>(BookingResponse.class, objectMapper);
        ErrorHandlingDeserializer<BookingResponse> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BookingResponse> replyContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, BookingResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(replyConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, BookingResponse> replyContainer() {
        ConcurrentMessageListenerContainer<String, BookingResponse> container =
                replyContainerFactory().createContainer("response_topic");
        container.getContainerProperties().setGroupId("reply-group");
        return container;
    }

    @Bean
    public ReplyingKafkaTemplate<String, BookingRequestDto, BookingResponse> replyingKafkaTemplate() {
        ReplyingKafkaTemplate<String, BookingRequestDto, BookingResponse> template =
                new ReplyingKafkaTemplate<>(requestProducerFactory(), replyContainer());
        template.setDefaultReplyTimeout(Duration.ofSeconds(30));
        template.setSharedReplyTopic(true);
        return template;
    }


}