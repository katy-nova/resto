package com.katynova.resto.booking.service.time_graph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppropriateBookingInfo<T> {
    // класс, хранящий инфу про слоты для возможного бронирования
    private int tableNumber;
    private int slotsBefore;
    private int slotsAfter;
    List<T> slots;
}
