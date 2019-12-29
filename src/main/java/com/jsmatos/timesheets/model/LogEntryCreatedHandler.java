package com.jsmatos.timesheets.model;

@FunctionalInterface
public interface LogEntryCreatedHandler {
    void onLogEntryCreated(LogEntry logEntry);
}
