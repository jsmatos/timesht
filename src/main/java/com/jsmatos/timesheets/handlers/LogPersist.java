package com.jsmatos.timesheets.handlers;

import com.jsmatos.timesheets.model.LogEntry;
import com.jsmatos.timesheets.model.LogEntryCreatedHandler;
import com.jsmatos.timesheets.storage.Repository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LogPersist implements LogEntryCreatedHandler {
    private final Repository repository;
    @Override
    public void onLogEntryCreated(LogEntry logEntry) {
        repository.save(logEntry);
        System.out.println(String.format("New log entered at %s", logEntry.getWhen()));
    }

}
