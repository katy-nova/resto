package com.katynova.resto.server_side.repository;

import com.katynova.resto.server_side.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Modifying
    @Query("DELETE FROM Booking b WHERE b.endTime < CURRENT_TIMESTAMP ")
     void deleteExpiredBookings();

    @Modifying
    @Query(value = "DELETE FROM bookings WHERE expired < NOW() AND bookings.status = 'PENDING' RETURNING id",
            nativeQuery = true)
    List<Long> deleteExpiredBookingsAndReturnIds();

}
