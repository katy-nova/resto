package com.katynova.resto.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDto {

    public int tableNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int persons;
    private String notes;
}
