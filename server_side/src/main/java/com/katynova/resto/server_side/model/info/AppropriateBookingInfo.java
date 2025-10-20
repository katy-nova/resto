package com.katynova.resto.server_side.model.info;

import com.katynova.resto.server_side.model.GraphSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppropriateBookingInfo {
    // класс, хранящий инфу про слоты для возможного бронирования
    private int tableNumber;
    private int slotsBefore;
    private int slotsAfter;
    List<GraphSlot> slots;
}
