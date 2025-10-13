package com.katynova.resto.booking.service;

import com.katynova.resto.booking.repository.BookingRepository;
import com.katynova.resto.booking.service.time_graph.config.TimeGraphRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingCleanUpService {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void cleanUp() {
        bookingRepository.deleteExpiredBookings();
        eventPublisher.publishEvent(new TimeGraphRefreshEvent(this));
    }
}
