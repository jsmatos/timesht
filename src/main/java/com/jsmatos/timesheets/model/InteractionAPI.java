package com.jsmatos.timesheets.model;

import java.util.List;

public interface InteractionAPI {
    List<LogEntry> getLogEntries(String filter, boolean groupByTask, int lastN);
    void publish(LogEntry logEntry);
    void publish(CloseEvent closeEvent);
    //ToDo: move to Screen
    RegistrationHandler registerLogEntryCreatedHandler(LogEntryCreatedHandler handler);
    RegistrationHandler registerVisibilityChangedHandler(VisibilityChangedHandler handler);
}
