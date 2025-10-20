package com.katynova.resto.server_side.model;


import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public record GraphSlot(
        LocalTime time,
        AtomicBoolean available,
        AtomicReference<Long> bookingId
) {

    public boolean isAvailable() {
        return available.get();
    }

    public LocalTime getTime() {
        return time;
    }

    public void book(Long id) {
        bookingId.set(id);
        available.set(false);
    }

    public void reserve() {
        bookingId.set(-1L);
        available.set(false);
    }

    public void unreserve() {
        bookingId.set(null);
        available.set(true);
    }

    // делаем equals только по времени, для варианта с suggest
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphSlot graphSlot = (GraphSlot) o;
        return time.equals(graphSlot.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

}
