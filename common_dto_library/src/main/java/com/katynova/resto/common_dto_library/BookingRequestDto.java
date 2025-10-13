package com.katynova.resto.booking.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import lombok.Data;
import jakarta.validation.constraints.Positive;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BookingRequestDto {
    private Long requestId;
    private String correlationId;
    private LocalDateTime startTime;
    private Duration duration;

    @Nullable
    private String notes;

    @Positive
    @Max(value = 8)
    private int persons;

    private Long guestId;
}
