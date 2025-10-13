package com.katynova.resto.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotConfirmation {
    private String correlationId;
    private Slot slot;
}
