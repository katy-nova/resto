package com.katynova.resto.server_side.utility_service;


import com.katynova.resto.server_side.config.TimeGraphRefreshEvent;
import com.katynova.resto.server_side.repository.BookingRepository;
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
