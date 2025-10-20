package com.katynova.resto.server_side.model;

import com.katynova.resto.server_side.model.status.ResponseStatus;
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
