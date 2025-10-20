package com.katynova.resto.server_side.service.service;

import com.katynova.resto.common_dto_library.BookingRequestDto;
import com.katynova.resto.server_side.model.FindResponse;
import com.katynova.resto.server_side.model.GraphSlot;
import com.katynova.resto.server_side.model.info.AppropriateBookingInfo;
import com.katynova.resto.server_side.model.info.SuggestBookingInfo;
import com.katynova.resto.server_side.model.status.ResponseStatus;
import com.katynova.resto.server_side.service.TimeGraphService;
import com.katynova.resto.server_side.utility_service.CapacityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = {"/tables_insert.sql", "/bookings_insert.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class TimeGraphServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("booking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TimeGraphService service;

    @Autowired
    private CapacityService capacityService;

    private BookingRequestDto bookingRequestDto;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setGuestId(2L);
        bookingRequestDto.setPersons(1);
        bookingDate = LocalDate.of(2025, 7, 15);
        capacityService.refreshCapacities();
        service.fillIn();
    }

    @Test
    void shouldGetIdealBooking() throws Exception {
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(12, 0)));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(12, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(13, 30));
        assertEquals(1, result.getTableNumber());
    }

    @Test
    void shouldGetBookingWithIdealEnd() throws Exception {
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(11, 0)));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(11, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(12, 30));
        assertEquals(2, result.getSlotsBefore());
        assertEquals(2, result.getTableNumber());
    }

    @Test
    void shouldGetBookingWithIdealStart() throws Exception {
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(20, 0)));
        bookingRequestDto.setDuration(Duration.of(1, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(2, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(20, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(20, 30));
        assertEquals(4, result.getSlotsAfter());
        assertEquals(3, result.getTableNumber());
    }

    @Test
    void shouldGetBookingWith2HoursEnd() throws Exception {
        bookingRequestDto.setPersons(3);
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(16, 0)));
        bookingRequestDto.setDuration(Duration.of(1, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(2, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(16, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(16, 30));
        assertEquals(7, result.getTableNumber());
    }

    @Test
    void shouldGetBookingWithIncreasedCapacity() throws Exception {
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(12, 0)));
        bookingRequestDto.setDuration(Duration.of(5, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(10, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(12, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(16, 30));
        assertEquals(7, result.getTableNumber());
    }

    @Test
    @Sql(scripts = {"/tables_insert.sql", "/bookings_insert.sql", "/clean_some_tables.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void shouldGetSuggestedBooking() throws Exception {
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(15, 0)));
        bookingRequestDto.setDuration(Duration.of(3, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(SuggestBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<SuggestBookingInfo> typedResponse =
                (FindResponse<SuggestBookingInfo>) response;
        assertEquals(ResponseStatus.SUGGESTED, response.getStatus());
        List<SuggestBookingInfo> result = typedResponse.getList().stream()
                .sorted(Comparator.comparing(info -> info.getSlots().getFirst().getTime())).toList();
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        List<GraphSlot> slots = result.getFirst().getSlots();
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(15, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(16, 30));
        assertEquals(3, result.getFirst().getTableNumber());

        slots = result.getLast().getSlots();
        assertEquals(6, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(16, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(18, 30));
    }


//    @Test
//    void shouldThrowManagerException() throws Exception {
//        bookingRequestDto.setPersons(10);
//        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate, LocalTime.of(13, 0)));
//        bookingRequestDto.setDuration(Duration.of(3, ChronoUnit.HOURS));
//        assertThrows(ManagerRequirementException.class, () -> service.findBooking(bookingRequestDto));
//    }
//
    @Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    void test() throws Exception {
        bookingRequestDto.setPersons(2);
        bookingRequestDto.setStartTime(LocalDateTime.of(bookingDate.minusDays(1), LocalTime.of(12, 0)));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(SuggestBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<SuggestBookingInfo> typedResponse =
                (FindResponse<SuggestBookingInfo>) response;
        assertEquals(ResponseStatus.SUGGESTED, response.getStatus());
        List<SuggestBookingInfo> result = typedResponse.getList().stream()
                .sorted(Comparator.comparing(info -> info.getSlots().getFirst().getTime())).toList();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        List<GraphSlot> slots = result.getFirst().getSlots();
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(11, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(12, 30));
        assertEquals(1, result.getFirst().getTableNumber());
    }

    @Sql(scripts = "/for_midnight_tests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    void shouldAddMidnightCrossBookingCorrectly() throws Exception {
        bookingRequestDto.setPersons(2);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7, 18, 23,0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(23, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(0, 30));
        assertEquals(1, result.getTableNumber());
    }

    @Sql(scripts = "/for_midnight_tests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    void shouldAddStartsAtMidnightBookingCorrectly() throws Exception {
        bookingRequestDto.setPersons(2);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7, 19, 0,0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(2, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(0, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(0, 30));
        assertEquals(1, result.getTableNumber());
    }

    @Sql(scripts = "/for_midnight_tests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    void shouldAddEndsAtMidnightBookingCorrectly() throws Exception {
        bookingRequestDto.setPersons(2);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7, 19, 22,0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(AppropriateBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<AppropriateBookingInfo> typedResponse =
                (FindResponse<AppropriateBookingInfo>) response;
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        AppropriateBookingInfo result = typedResponse.getList().getFirst();
        List<GraphSlot> slots = result.getSlots();
        assertFalse(slots.isEmpty());
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(22, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(23, 30));
        assertEquals(1, result.getTableNumber());
    }

    @Sql(scripts = "/for_midnight_tests.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    void shouldAddMidnightSuggestBookingCorrectly() throws Exception {
        bookingRequestDto.setPersons(4);
        bookingRequestDto.setStartTime(LocalDateTime.of(2025, 7, 18, 22,0));
        bookingRequestDto.setDuration(Duration.of(2, ChronoUnit.HOURS));
        FindResponse<?> response = service.findBooking(bookingRequestDto);
        assertFalse(response.getList().isEmpty());
        assertInstanceOf(SuggestBookingInfo.class, response.getList().getFirst());
        @SuppressWarnings("unchecked")
        FindResponse<SuggestBookingInfo> typedResponse =
                (FindResponse<SuggestBookingInfo>) response;
        assertEquals(ResponseStatus.SUGGESTED, response.getStatus());
        List<SuggestBookingInfo> result = typedResponse.getList().stream()
                .sorted(Comparator.comparing(info -> info.getSlots().getFirst().getTime())).toList();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        List<GraphSlot> slots = result.getFirst().getSlots();
        assertEquals(4, slots.size());
        assertEquals(slots.getFirst().getTime(), LocalTime.of(23, 0));
        assertEquals(slots.getLast().getTime(), LocalTime.of(0, 30));
        assertEquals(2, result.getFirst().getTableNumber());
    }

}
