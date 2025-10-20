package com.katynova.resto.common_dto_library.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Slot {
    private Long bookingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
