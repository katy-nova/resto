package com.katynova.resto.common_dto_library.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class BookingSuccessResponse extends BookingResponse {

    public BookingSuccessResponse(String correlationId, Long requestId) {
        super(correlationId, requestId);
    }
}
