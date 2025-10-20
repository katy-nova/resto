package com.katynova.resto.server_side.service.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.common_dto_library.response.BookingResponse;
import com.katynova.resto.common_dto_library.response.BookingSuggestResponse;
import com.katynova.resto.common_dto_library.response.Slot;
import com.katynova.resto.server_side.model.FindResponse;
import com.katynova.resto.server_side.model.GraphSlot;
import com.katynova.resto.server_side.model.entity.RestTable;
import com.katynova.resto.server_side.model.info.SuggestBookingInfo;
import com.katynova.resto.server_side.model.status.ResponseStatus;
import com.katynova.resto.server_side.repository.BookingRepository;
import com.katynova.resto.server_side.repository.TableRepository;
import com.katynova.resto.server_side.service.BookingTimeGraphService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingTimeGraphServiceTest {

    @InjectMocks
    BookingTimeGraphService bookingTimeGraphService;

    @Mock
    TableRepository tableRepository;

    @Mock
    BookingRepository bookingRepository;

    BookingRequestDto bookingRequestDto;

    @Test
    void shouldGetSuggestResponse() {
        bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setGuestId(2L);
        bookingRequestDto.setPersons(1);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7,21, 14, 0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        GraphSlot slot1 = new GraphSlot(LocalTime.of(13, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot2 = new GraphSlot(LocalTime.of(13, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot3 = new GraphSlot(LocalTime.of(14, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot4 = new GraphSlot(LocalTime.of(14, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        List<GraphSlot> slots = List.of(slot1, slot2, slot3, slot4);
        SuggestBookingInfo suggestBookingInfo = new SuggestBookingInfo();
        suggestBookingInfo.setSlots(slots);
        suggestBookingInfo.setTableNumber(1);
        FindResponse<SuggestBookingInfo> response = new FindResponse<>(List.of(suggestBookingInfo), ResponseStatus.SUGGESTED);
        RestTable restTable = new RestTable();
        restTable.setTableNumber(1);
        restTable.setCapacity(2);
        when(tableRepository.getReferenceById(1)).thenReturn(restTable);
        when(bookingRepository.save(any())).thenReturn(null);
        BookingResponse bookingResponse = bookingTimeGraphService.sendSuggestResponse(bookingRequestDto, response);
        assertInstanceOf(BookingSuggestResponse.class, bookingResponse);
        BookingSuggestResponse suggestResponse = (BookingSuggestResponse) bookingResponse;
        List<Slot> bookingSlots = suggestResponse.getSlots();
        assertEquals(1, bookingSlots.size());
        Slot slot = bookingSlots.getFirst();
        assertEquals(LocalDateTime.of(2025, 7, 21, 13, 0), slot.getStartTime());
        assertEquals(LocalDateTime.of(2025, 7, 21, 15, 0), slot.getEndTime());
    }

    @Test
    void shouldGetMidnightCrossStartsBeforeSuggestResponse() {
        bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setGuestId(2L);
        bookingRequestDto.setPersons(1);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7,18, 22, 0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        GraphSlot slot1 = new GraphSlot(LocalTime.of(23, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot2 = new GraphSlot(LocalTime.of(23, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot3 = new GraphSlot(LocalTime.of(0, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot4 = new GraphSlot(LocalTime.of(0, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        List<GraphSlot> slots = List.of(slot1, slot2, slot3, slot4);
        SuggestBookingInfo suggestBookingInfo = new SuggestBookingInfo();
        suggestBookingInfo.setSlots(slots);
        suggestBookingInfo.setTableNumber(1);
        RestTable restTable = new RestTable();
        restTable.setTableNumber(1);
        restTable.setCapacity(2);
        when(tableRepository.getReferenceById(1)).thenReturn(restTable);
        when(bookingRepository.save(any())).thenReturn(null);
        FindResponse<SuggestBookingInfo> response = new FindResponse<>(List.of(suggestBookingInfo), ResponseStatus.SUGGESTED);
        BookingResponse bookingResponse = bookingTimeGraphService.sendSuggestResponse(bookingRequestDto, response);
        assertInstanceOf(BookingSuggestResponse.class, bookingResponse);
        BookingSuggestResponse suggestResponse = (BookingSuggestResponse) bookingResponse;
        List<Slot> bookingSlots = suggestResponse.getSlots();
        assertEquals(1, bookingSlots.size());
        Slot slot = bookingSlots.getFirst();
        assertEquals(LocalDateTime.of(2025, 7, 18, 23, 0), slot.getStartTime());
        assertEquals(LocalDateTime.of(2025, 7, 19, 1, 0), slot.getEndTime());
    }
    @Test
    void shouldGetMidnightCrossStartsAfterSuggestResponse() {
        bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setGuestId(2L);
        bookingRequestDto.setPersons(1);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7,19, 0, 0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        GraphSlot slot1 = new GraphSlot(LocalTime.of(23, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot2 = new GraphSlot(LocalTime.of(23, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot3 = new GraphSlot(LocalTime.of(0, 0), new AtomicBoolean(false), new AtomicReference<>(-1L));
        GraphSlot slot4 = new GraphSlot(LocalTime.of(0, 30), new AtomicBoolean(false), new AtomicReference<>(-1L));
        List<GraphSlot> slots = List.of(slot1, slot2, slot3, slot4);
        SuggestBookingInfo suggestBookingInfo = new SuggestBookingInfo();
        suggestBookingInfo.setSlots(slots);
        suggestBookingInfo.setTableNumber(1);
        RestTable restTable = new RestTable();
        restTable.setTableNumber(1);
        restTable.setCapacity(2);
        when(tableRepository.getReferenceById(1)).thenReturn(restTable);
        when(bookingRepository.save(any())).thenReturn(null);
        FindResponse<SuggestBookingInfo> response = new FindResponse<>(List.of(suggestBookingInfo), ResponseStatus.SUGGESTED);
        BookingResponse bookingResponse = bookingTimeGraphService.sendSuggestResponse(bookingRequestDto, response);
        assertInstanceOf(BookingSuggestResponse.class, bookingResponse);
        BookingSuggestResponse suggestResponse = (BookingSuggestResponse) bookingResponse;
        List<Slot> bookingSlots = suggestResponse.getSlots();
        assertEquals(1, bookingSlots.size());
        Slot slot = bookingSlots.getFirst();
        assertEquals(LocalDateTime.of(2025, 7, 18, 23, 0), slot.getStartTime());
        assertEquals(LocalDateTime.of(2025, 7, 19, 1, 0), slot.getEndTime());
    }
}