package com.katynova.resto.common_dto_library;

import com.katynova.resto.common_dto_library.validation.CorrectDuration;
import com.katynova.resto.common_dto_library.validation.CorrectStartTime;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BookingRequestDto {
    @Nullable
    private Long requestId;

    @Nullable
    private String correlationId;

    @NotNull
    @Future
    @CorrectStartTime
    private LocalDateTime startTime;

    @NotNull
    @Positive
    @CorrectDuration
    private Duration duration;

    @Nullable
    private String notes;

    @Positive
    @NotNull
    @Max(value = 8, message = "Для бронирования стола больше, чем на 8 человек свяжитесь с менеджером")
    private int persons;

    @NotNull
    @Positive
    private Long guestId;
}
