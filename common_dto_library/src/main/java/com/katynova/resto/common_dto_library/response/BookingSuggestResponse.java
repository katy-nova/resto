package com.katynova.resto.common_dto_library.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BookingSuggestResponse extends BookingResponse {
    private List<Slot> slots;
    private LocalDateTime expired;

    public BookingSuggestResponse(String correlationId, Long requestId, List<Slot> slots, LocalDateTime expired) {
        super(correlationId, requestId);
        this.slots = slots;
        this.expired = expired;
    }
}
