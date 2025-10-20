package com.katynova.resto.server_side.service.service;

import com.katynova.resto.server_side.model.GraphSlot;
import com.katynova.resto.server_side.service.TimeGraphService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = {"/filling_in_test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class FillingInIntegrationTest {

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
    private TimeGraphService timeGraphService;

    private ConcurrentMap<LocalDate, ConcurrentMap
            <Integer, ConcurrentMap
                    <Integer, List<GraphSlot>>>> timeGraph;
    private final int firstCapacity = 2;
    private final int secondCapacity = 4;

    @Test
    void shouldFillInGraph() {
        timeGraphService.fillIn();
        timeGraph = timeGraphService.getTimeGraph();
        assertNotNull(timeGraph);
        assertFalse(timeGraph.isEmpty());
        LocalDate firstDay = LocalDate.of(2025, 7, 13);
        // достаем день
        ConcurrentMap<Integer, ConcurrentMap<Integer, List<GraphSlot>>> firstDayMap = timeGraph.get(firstDay);
        assertNotNull(firstDayMap);
        assertFalse(firstDayMap.isEmpty());
        assertEquals(2, firstDayMap.size());
        // достаем capacity = 2
        ConcurrentMap<Integer, List<GraphSlot>> firstDayFirstCapacityMap = firstDayMap.get(firstCapacity);
        assertNotNull(firstDayFirstCapacityMap);
        assertFalse(firstDayFirstCapacityMap.isEmpty());
        assertEquals(2, firstDayFirstCapacityMap.size());
        // достаем capacity = 4
        ConcurrentMap<Integer, List<GraphSlot>> firstDaySecondCapacityMap = firstDayMap.get(secondCapacity);
        assertNotNull(firstDaySecondCapacityMap);
        assertFalse(firstDaySecondCapacityMap.isEmpty());
        assertEquals(1, firstDaySecondCapacityMap.size());
        // достаем стол
        List<GraphSlot> firstDayFirstCapacity = firstDayFirstCapacityMap.get(1);
        assertNotNull(firstDayFirstCapacity);
        assertFalse(firstDayFirstCapacity.isEmpty());
        assertEquals(26, firstDayFirstCapacity.size());
        GraphSlot bookingFirstSlot = firstDayFirstCapacity.get(6);
        assertEquals(LocalTime.of(13,0), bookingFirstSlot.getTime());
        assertFalse(bookingFirstSlot.isAvailable());
        GraphSlot bookingSecondSlot = firstDayFirstCapacity.get(7);
        assertEquals(LocalTime.of(13,30), bookingSecondSlot.getTime());
        assertFalse(bookingSecondSlot.isAvailable());
        GraphSlot firstAvailable = firstDayFirstCapacity.get(8);
        assertTrue(firstAvailable.isAvailable());
    }

    @Test
    void shouldCorrectlyFillInBookingAtMidnight() {
        timeGraphService.fillIn();
        timeGraph = timeGraphService.getTimeGraph();
        List<GraphSlot> slots = timeGraph.get(LocalDate.of(2025, 7,18)).get(4).get(3);
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        assertEquals(30, slots.size());

        GraphSlot bookingFirstSlot = slots.get(26);
        assertEquals(LocalTime.of(23,0), bookingFirstSlot.getTime());
        assertFalse(bookingFirstSlot.isAvailable());

        GraphSlot bookingSecondSlot = slots.get(27);
        assertEquals(LocalTime.of(23,30), bookingSecondSlot.getTime());
        assertFalse(bookingSecondSlot.isAvailable());

        GraphSlot bookingThirdSlot = slots.get(28);
        assertEquals(LocalTime.of(0,0), bookingThirdSlot.getTime());
        assertFalse(bookingThirdSlot.isAvailable());

        GraphSlot bookingFourthSlot = slots.get(29);
        assertEquals(LocalTime.of(0,30), bookingFourthSlot.getTime());
    }
}
