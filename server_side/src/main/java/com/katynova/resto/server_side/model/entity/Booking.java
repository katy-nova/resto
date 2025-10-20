package com.katynova.resto.server_side.model.entity;

import com.katynova.resto.server_side.model.status.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "booking_sequence", allocationSize = 50)
    private Long id;

    // как будто нет смысла хранить тут сущность пользователя
    private Long guestId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_number", nullable = false)
    private RestTable restTable;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int persons;
    private String notes;

    @CreationTimestamp
    private Instant created;

    private LocalDateTime expired;

    public Integer getTableNumber() {
        return restTable.getTableNumber();
    }
}
