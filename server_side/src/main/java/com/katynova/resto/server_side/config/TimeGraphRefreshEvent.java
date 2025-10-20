package com.katynova.resto.server_side.config;


import org.springframework.context.ApplicationEvent;

public class TimeGraphRefreshEvent extends ApplicationEvent {
    public TimeGraphRefreshEvent(Object source) {
        super(source);
    }
}