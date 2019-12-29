package com.jsmatos.timesheets.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LogEntry {
    private final Date when;
    private final String what;


    @Override
    public String toString() {
        return String.format("[ %s ] %s", when, what);
    }
}
