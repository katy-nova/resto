package com.katynova.resto.booking.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class BookingSuccessResponse extends BookingResponse {

    public BookingSuccessResponse(String correlationId, Long requestId) {
        super(correlationId, requestId);
    }
}
