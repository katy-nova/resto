package com.katynova.resto.booking.service.raw_booking_sql.model;

import com.katynova.resto.booking.model.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindResponse<T> {
    private List<T> list;
    private ResponseStatus status;
}
