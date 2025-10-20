
import com.katynova.resto.server_side.config.WorkTimeConfig;
import com.katynova.resto.server_side.utility_service.WorkingHoursCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimeWorkingTest {

    @InjectMocks
    private WorkingHoursCounter workingHoursCounter;

    @Mock
    private WorkTimeConfig workTimeConfig;


    @BeforeEach
    void setUp() {
        when(workTimeConfig.getOpenTime()).thenReturn(LocalTime.of(10, 0));
    }

    @Test
    void shouldGetOpenDateWithStartToday() {
        LocalDateTime startToday = LocalDateTime.of(2025, 7, 13, 14,0);
        LocalDateTime openDate = workingHoursCounter.getOpenDateTime(startToday);
        assertEquals(openDate, LocalDateTime.of(2025, 7, 13, workTimeConfig.getOpenTime().getHour(),0));
    }

    @Test
    void shouldGetOpenDateWithStartTomorrow() {
        LocalDateTime startTomorrow = LocalDateTime.of(2025, 7, 14, 1,0);
        LocalDateTime openDate = workingHoursCounter.getOpenDateTime(startTomorrow);
        assertEquals(openDate, LocalDateTime.of(2025, 7, 13, workTimeConfig.getOpenTime().getHour(),0));
    }

    @Test
    void shouldGetCloseDateWeekdayWithStartToday() {
        when(workTimeConfig.getCloseWeekdaysTime()).thenReturn(LocalTime.of(23, 0));
        when(workTimeConfig.getCloseWeekendTime()).thenReturn(LocalTime.of(1, 0));
        LocalDateTime endToday = LocalDateTime.of(2025, 7, 13, 14,0); // sunday
        LocalDateTime closeDate = workingHoursCounter.getCloseDateTime(endToday);
        assertEquals(closeDate, LocalDateTime.of(2025, 7, 13, workTimeConfig.getCloseWeekdaysTime().getHour(),0));
    }

    @Test
    void shouldGetCloseDateWeekendWithStartToday() {
        when(workTimeConfig.getCloseWeekdaysTime()).thenReturn(LocalTime.of(23, 0));
        when(workTimeConfig.getCloseWeekendTime()).thenReturn(LocalTime.of(1, 0));
        LocalDateTime endToday = LocalDateTime.of(2025, 7, 12, 14,0); // saturday
        LocalDateTime closeDate = workingHoursCounter.getCloseDateTime(endToday);
        assertEquals(closeDate, LocalDateTime.of(2025, 7, 13, workTimeConfig.getCloseWeekendTime().getHour(),0));
    }

    @Test
    void shouldGetCloseDateWeekendWithStartTomorrow() {
        when(workTimeConfig.getCloseWeekdaysTime()).thenReturn(LocalTime.of(23, 0));
        when(workTimeConfig.getCloseWeekendTime()).thenReturn(LocalTime.of(1, 0));
        LocalDateTime endToday = LocalDateTime.of(2025, 7, 13, 1,0); // saturday
        LocalDateTime closeDate = workingHoursCounter.getCloseDateTime(endToday);
        assertEquals(closeDate, LocalDateTime.of(2025, 7, 13, workTimeConfig.getCloseWeekendTime().getHour(),0));
    }

    @Test
    void getCloseDateTime_thursdayNight_shouldUseWeekdayClosing() {
        when(workTimeConfig.getCloseWeekdaysTime()).thenReturn(LocalTime.of(1, 0));
        when(workTimeConfig.getCloseWeekendTime()).thenReturn(LocalTime.of(3, 0));
        LocalDateTime input = LocalDateTime.of(2025, 7, 14, 0, 0); // Пятница 00:00 (ночь с чт на пт)
        LocalDateTime result = workingHoursCounter.getCloseDateTime(input);
        assertEquals(LocalDateTime.of(2025, 7, 14, 1, 0), result);
    }

    @Test
    void getCloseDateTime_fridayEarlyMorning_shouldUseWeekendClosing() {
        when(workTimeConfig.getCloseWeekdaysTime()).thenReturn(LocalTime.of(1, 0));
        when(workTimeConfig.getCloseWeekendTime()).thenReturn(LocalTime.of(3, 0));
        LocalDateTime input = LocalDateTime.of(2025, 7, 18, 1, 0); // Пятница 00:01
        LocalDateTime result = workingHoursCounter.getCloseDateTime(input);
        assertEquals(LocalDateTime.of(2025, 7, 18, 1, 0), result);
    }

}
