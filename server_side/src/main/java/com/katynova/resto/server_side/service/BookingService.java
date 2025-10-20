package com.katynova.resto.server_side.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.SlotConfirmation;

public interface BookingService {

    BookingResponse getResponse(BookingRequestDto bookingRequestDto);
    BookingResponse confirmSlot(SlotConfirmation slot);
}
