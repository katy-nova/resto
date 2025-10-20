package com.katynova.resto.common_dto_library.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotConfirmation {
    @NotNull
    @NotBlank
    private String correlationId;

    @NotNull
    @Positive
    private Long requestId;

    @NotNull
    @Positive
    private Long confirmSlotId;

    private List<Long> rejectedSlotIds;
}
