package com.jsmatos.timesheets.handlers;

import com.jsmatos.timesheets.model.LogEntry;
import com.jsmatos.timesheets.model.LogEntryCreatedHandler;
import com.jsmatos.timesheets.storage.EntryRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LogPersist implements LogEntryCreatedHandler {
    private final EntryRepository entryRepository;
    @Override
    public void onLogEntryCreated(LogEntry logEntry) {
        entryRepository.save(logEntry);
        System.out.println(String.format("New log entered at %s", logEntry.getWhen()));
    }

}
