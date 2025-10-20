package com.katynova.resto.server_side.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
// отработает только если будет выбрана эта реализации сервиса
@ConditionalOnClass(TimeGraphService.class)
@Slf4j
public class TimeGraphInitializer {

    private final TimeGraphService timeGraphService;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing time graph cache...");
            timeGraphService.fillIn();
            log.info("Time graph cache initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize time graph cache", e);
            throw new IllegalStateException("Time graph initialization failed", e);
        }
    }
}
