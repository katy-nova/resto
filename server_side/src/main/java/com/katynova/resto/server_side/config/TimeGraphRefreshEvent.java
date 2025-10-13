package com.katynova.resto.booking.service.time_graph.config;


import org.springframework.context.ApplicationEvent;

public class TimeGraphRefreshEvent extends ApplicationEvent {
    public TimeGraphRefreshEvent(Object source) {
        super(source);
    }
}