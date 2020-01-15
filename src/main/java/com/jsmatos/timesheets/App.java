package com.jsmatos.timesheets;

import com.jsmatos.timesheets.gui.Alert;
import com.jsmatos.timesheets.gui.Screen;
import com.jsmatos.timesheets.handlers.LogPersist;
import com.jsmatos.timesheets.model.*;
import com.jsmatos.timesheets.storage.StatusRepository;
import com.jsmatos.timesheets.storage.EntryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class App implements InteractionAPI {
    private final List<VisibilityChangedHandler> visibilityChangedHandlers = new ArrayList<>();
    private final EntryRepository entryRepository;
    private final LogPersist logPersist;
    private final Timer timer = new Timer(false);
    private List<LogEntryCreatedHandler> logEntryCreatedHandlers = new ArrayList<>();
    private volatile boolean running = true;
    private TimerTask makeVisibleTask;

    public App() {
        this.entryRepository = new EntryRepository();
        this.logPersist = new LogPersist(entryRepository);
    }

    public static void main(String[] args) {
        App app = new App();
        app.init();
    }

    @Override
    public List<LogEntry> getLogEntries(String filter, boolean groupByTask, int lastN) {
        return this.entryRepository.findByFilter(filter, groupByTask, lastN);
    }

    @Override
    public void publish(LogEntry logEntry) {
        scheduleVisibility(30);
        logEntryCreatedHandlers.forEach(le -> le.onLogEntryCreated(logEntry));
    }

    @Override
    public void publish(CloseEvent closeEvent) {
        scheduleVisibility(10);
    }

    @Override
    public RegistrationHandler registerLogEntryCreatedHandler(LogEntryCreatedHandler handler) {
        System.out.println("App.registerLogEntryCreatedHandler");
        logEntryCreatedHandlers.add(handler);
        return () -> logEntryCreatedHandlers.remove(handler);
    }

    @Override
    public RegistrationHandler registerVisibilityChangedHandler(VisibilityChangedHandler handler) {
        System.out.println("App.registerVisibilityChangedHandler");
        visibilityChangedHandlers.add(handler);
        return () -> visibilityChangedHandlers.remove(handler);
    }

    private void init() {
        StatusRepository statusRepository = new StatusRepository();
        Status status = statusRepository.getStatus();
        if (status.isRunning()) {
            Optional<Integer> port = status.getPort();
            if (port.isPresent()) {
                sendShowUpCommand(port.get());
            } else {
                Alert.warn("Timeshts", "Couldn't connect to possible running instance, no port found! Please check.");
            }
        } else {
            regularInit();
            startCommandServer(statusRepository);
        }
    }

    private void sendShowUpCommand(int portNumber) {
        try (
                Socket echoSocket = new Socket("localhost", portNumber);
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
        ) {
            out.println("show");
        } catch (IOException e) {
            Alert.error("Timeshts", String.format("Unable to send command to running application on port %s: %s", portNumber, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void regularInit() {
        Screen.startInstance(this);
        logEntryCreatedHandlers.add(logPersist);
        scheduleVisibility(0, 30);
    }

    private void scheduleVisibility(int after) {
        scheduleVisibility(after,30);
    }
    private void scheduleVisibility(int after, int minutes) {
        if(makeVisibleTask!=null){
            makeVisibleTask.cancel();
        }
        timer.purge();
        makeVisibleTask = new TimerTask() {
            @Override
            public void run() {
                makeVisible();
            }
        };
        long period = TimeUnit.MINUTES.toMillis(minutes);
        long delay = TimeUnit.MINUTES.toMillis(after);
        timer.schedule(makeVisibleTask, delay, period);
    }

    private void makeVisible() {
        visibilityChangedHandlers.forEach(h -> h.onVisibilityChanged(true));
    }

    private void startCommandServer(StatusRepository pid) {
        Runnable r = () -> {
            int portNumber = Double.valueOf(Math.random() * 4096 + 1024).intValue();
            try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
                System.out.println(String.format("Listening for commands on port %d", portNumber));
                pid.setStatus(portNumber);
                while (running) {
                    handleClient(serverSocket);
                }
            } catch (IOException e) {
                Alert.error("Timeshts", String.format("Unable to start command server: %s", e.getMessage()));
                e.printStackTrace();
            }
        };
        new Thread(r).start();
    }

    private void handleClient(ServerSocket serverSocket) {
        try (
                Socket clientSocket = serverSocket.accept();
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()))) {

            final String command = in.readLine();
            if ("show".equals(command)) {
                makeVisible();
                out.println("ok");
            } else {
                String msg = String.format("Unknown command: %s", command);
                System.out.println(msg);
                out.println(msg);
            }

        } catch (IOException e) {
            Alert.warn("Timeshts", String.format("Error handling command: %s", e.getMessage()));
            e.printStackTrace();
        }
    }
}
