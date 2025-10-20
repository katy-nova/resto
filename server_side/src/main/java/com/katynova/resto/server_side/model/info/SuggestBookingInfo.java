package com.katynova.resto.server_side.model.info;

import com.katynova.resto.server_side.model.GraphSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuggestBookingInfo {
    private int tableNumber;
    private List<GraphSlot> slots;

    // объекты этого класса будут считаться равными, если у них одинаковые слоты по времени
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestBookingInfo that = (SuggestBookingInfo) o;
        return Objects.equals(slots, that.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slots);
    }
}
