package com.katynova.resto.booking.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WaitlistResponse extends BookingResponse {
    // для этого класса потом будет какая-то логика добавления в очередь
    private int queuePosition;

    public WaitlistResponse(String correlationId, Long requestId, int queuePosition) {
        super(correlationId, requestId);
        this.queuePosition = queuePosition;
    }
}
