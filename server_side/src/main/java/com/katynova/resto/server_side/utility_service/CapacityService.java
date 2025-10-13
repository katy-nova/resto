package com.katynova.resto.booking.service;

import com.katynova.resto.booking.exception.ManagerRequirementException;
import com.katynova.resto.booking.repository.TableRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CapacityService {

    private final TableRepository tableRepository;

    private volatile List<Integer> capacities;

    public List<Integer> getCapacities() {
        if (capacities == null) {
            synchronized (this) {
                init();
            }
        }
        return capacities;
    }

    @PostConstruct
    public void init() {
        List<Integer> caps = tableRepository.findDistinctCapacity();
        capacities = List.copyOf(caps);
        log.info("Capacities: {}", capacities);
    }

    // применяем такую строгую пессимистическую блокировку, тк предполагаем, что обновляться таблица будет крайне редко
    public synchronized void refreshCapacities() {
        List<Integer> caps = tableRepository.findDistinctCapacity();
        capacities = List.copyOf(caps);
        log.info("Capacities refreshed: {}", capacities);
    }

    public int getCapacity(int persons) throws ManagerRequirementException {
        for (Integer capacity : capacities) {
            if (capacity >= persons) {
                return capacity;
            }
        }
        throw new ManagerRequirementException("Бронирование стола более чем на " + capacities.stream().max(Integer::compareTo).get() + " человек не производится в" +
                " автоматическом режиме. С вами свяжется менеджер ресторана для подтверждения бронирования");
    }
}
