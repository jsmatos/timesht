package com.jsmatos.timesheets.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LogEntry {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/YYYY HH:mm");

    private final Date when;
    private final String what;


    @Override
    public String toString() {
        return String.format("%s| %s", DATE_FORMAT.format(when), what);
    }
}
