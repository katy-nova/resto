package com.katynova.resto.common_dto_library.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookingErrorResponse extends BookingResponse {

    private String error;

    public BookingErrorResponse(String correlationId, Long requestId, String error) {
        super(correlationId, requestId);
        this.error = error;
    }
}
