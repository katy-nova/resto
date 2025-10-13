package com.katynova.resto.booking.repository;

import com.katynova.resto.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.startTime > :startDay AND b.endTime < :endDay AND b.restTable.tableNumber IN " +
            "(SELECT t.tableNumber FROM RestTable t " +
            "WHERE t.capacity = :capacity AND NOT EXISTS (" +
            "SELECT 1 FROM Booking b2 WHERE b2.restTable = t " +
            "AND b2.startTime < :endTime AND b2.endTime > :startTime) )")
    List<Booking> findByQuery(@Param("startDay") LocalDateTime startDay, @Param("endDay") LocalDateTime endDay,
                                     @Param("startTime") LocalDateTime start, @Param("endTime") LocalDateTime end,
                                     @Param("capacity") int capacity);

    @Modifying
    @Query("DELETE FROM Booking b WHERE b.endTime < CURRENT_TIMESTAMP ")
     void deleteExpiredBookings();

    @Query("SELECT b FROM Booking b " +
            "WHERE b.startTime >= :startDay AND b.endTime <= :endDay AND b.restTable.capacity = :capacity")
    List<Booking> findByDateAndCapacity(@Param("startDay") LocalDateTime startDay,
                                        @Param("endDay") LocalDateTime endDay,
                                        @Param("capacity") int capacity);



}
