package com.katynova.resto.server_side.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "restaurant_tables")
public class RestTable {

    @Id
    @JoinColumn(name = "table_number")
    private int tableNumber;

    private int capacity;

    private String note;

    @OneToMany(mappedBy = "restTable")
    private Set<Booking> bookings;
}
