package com.katynova.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
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
import com.katynova.dto.data.HotDataDto;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaRequestReplyConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, List<Long>> requestProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, List<HotDataDto>> replyConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "reply-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.katynova.dto,java.util,java.lang");

        JsonDeserializer<List<HotDataDto>> valueDeserializer = new JsonDeserializer<>() {
            @Override
            public List<HotDataDto> deserialize(String topic, byte[] data) {
                try {
                    JavaType type = objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, HotDataDto.class);
                    return objectMapper.readValue(data, type);
                } catch (Exception e) {
                    throw new SerializationException("Error deserializing HotDataDto list", e);
                }
            }
        };
        ErrorHandlingDeserializer<List<HotDataDto>> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, List<HotDataDto>> replyContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<HotDataDto>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(replyConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, List<HotDataDto>> replyContainer() {
        ConcurrentMessageListenerContainer<String, List<HotDataDto>> container =
                replyContainerFactory().createContainer("data_response");
        container.getContainerProperties().setGroupId("reply-group"); // УСТАНОВИТЬ ГРУППУ
        return container;
    }

    @Bean
    public ReplyingKafkaTemplate<String, List<Long>, List<HotDataDto>> replyingKafkaTemplate() {
        ReplyingKafkaTemplate<String, List<Long>, List<HotDataDto>> template =
                new ReplyingKafkaTemplate<>(requestProducerFactory(), replyContainer());
        template.setDefaultReplyTimeout(Duration.ofSeconds(30));
        template.setSharedReplyTopic(true);
        return template;
    }
}