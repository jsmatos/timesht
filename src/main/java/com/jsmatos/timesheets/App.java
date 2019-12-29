package com.jsmatos.timesheets;

import com.jsmatos.timesheets.gui.Screen;
import com.jsmatos.timesheets.handlers.LogPersist;
import com.jsmatos.timesheets.model.RegistrationHandler;
import com.jsmatos.timesheets.model.VisibilityChangedHandler;
import com.jsmatos.timesheets.model.*;
import com.jsmatos.timesheets.storage.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class App implements InteractionAPI {
    private List<LogEntryCreatedHandler> logEntryCreatedHandlers = new ArrayList<>();
    private final List<VisibilityChangedHandler> visibilityChangedHandlers = new ArrayList<>();
    private final Repository repository;
    private final LogPersist logPersist;

    public App() {
        this.repository = new Repository();
        this.logPersist = new LogPersist(repository);
    }

    @Override
    public List<LogEntry> getLogEntries(String filter) {
        return this.repository.findBy(filter);
    }

    @Override
    public void publish(LogEntry logEntry) {
        logEntryCreatedHandlers.forEach(le -> le.onLogEntryCreated(logEntry));
    }

    @Override
    public void publish(CloseEvent closeEvent) {
    }

    @Override
    public RegistrationHandler registerLogEntryCreatedHandler(LogEntryCreatedHandler handler) {
        logEntryCreatedHandlers.add(handler);
        return () -> logEntryCreatedHandlers.remove(handler);
    }

    @Override
    public RegistrationHandler registerVisibilityChangedHandler(VisibilityChangedHandler handler) {
        System.out.println("App.registerVisibilityChangedHandler");
        visibilityChangedHandlers.add(handler);
        return () -> visibilityChangedHandlers.remove(handler);
    }

    public static void main(String[] args) {
        App app = new App();
        app.init();
        Screen.startInstance(app);


    }

    private void init() {
        logEntryCreatedHandlers.add(logPersist);
        Timer timer = new Timer(false);
        long delay = 0;
        long period = TimeUnit.SECONDS.toMillis(30);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                visibilityChangedHandlers.forEach(h -> h.onVisibilityChanged(true));
            }
        };
        timer.schedule(task, delay, period);
    }

}
