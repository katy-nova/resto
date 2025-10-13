package com.katynova.resto.booking.repository;

import com.katynova.resto.booking.model.RestTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TableRepository extends JpaRepository<RestTable, Integer> {

    @Query("SELECT DISTINCT t.capacity FROM RestTable t ORDER BY t.capacity ASC ")
    List<Integer> findDistinctCapacity();

    Optional<RestTable> findByTableNumber(int tableNumber);

    List<RestTable> findByTableNumberIn(Set<Integer> tableNumbers);

    @Query("SELECT t.tableNumber FROM RestTable t WHERE NOT EXISTS " +
            "(SELECT 1 FROM Booking b WHERE b.restTable.tableNumber = t.tableNumber " +
            "AND b.startTime >= :startDay AND b.endTime <= :endDay) AND t.capacity = :capacity")
    List<Integer> findTableWithoutBookings(@Param("startDay") LocalDateTime startDay,
                                           @Param("endDay") LocalDateTime endDay,
                                           @Param("capacity") int capacity);


    @Query("SELECT DISTINCT t FROM RestTable t LEFT JOIN FETCH t.bookings")
    List<RestTable> findAllWithBookings();
}
