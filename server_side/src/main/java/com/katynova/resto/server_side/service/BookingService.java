package com.katynova.resto.booking.service;

import com.katynova.resto.booking.dto.BookingRequestDto;
import com.katynova.resto.booking.dto.response.BookingResponse;
import com.katynova.resto.booking.dto.response.SlotConfirmation;

public interface BookingService {

    BookingResponse getResponse(BookingRequestDto bookingRequestDto);
    void removeSuggestedBookings(BookingResponse response);
    BookingResponse confirmSlot(SlotConfirmation slot, BookingResponse response, BookingRequestDto bookingRequestDto);
}
