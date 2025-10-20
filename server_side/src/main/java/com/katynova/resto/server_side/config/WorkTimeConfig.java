package com.katynova.resto.server_side.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "worktime")
@Getter
@Setter
public class WorkTimeConfig {
    private LocalTime openTime;
    private LocalTime closeWeekdaysTime;
    private LocalTime closeWeekendTime;
}
