package com.katynova.resto.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookingSuccessResponse.class, name = "SUCCESS"),
        @JsonSubTypes.Type(value = BookingSuggestResponse.class, name = "SUGGESTED"),
        @JsonSubTypes.Type(value = WaitlistResponse.class, name = "WAITLIST"),
        @JsonSubTypes.Type(value = BookingErrorResponse.class, name = "ERROR")
})

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class BookingResponse {
    private String correlationId;
    private Long requestId;
}
