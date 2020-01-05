package com.jsmatos.timesheets.model;

import java.util.Optional;

public class Status {
    private final boolean running;
    private final Long pid;
    private final Integer onPort;

    public static Status NOT_RUNNING = new Status(false, null,null);

    private Status(boolean running, Long pid, Integer onPort) {
        this.running = running;
        this.pid = pid;
        this.onPort = onPort;
    }

    public boolean isRunning() {
        return this.running;
    }

    public static Status runningOnPort(long pid, int port) {
        return new Status(true, pid, port);
    }

    public Optional<Integer> getPort() {
        return Optional.ofNullable(this.onPort);
    }
    public Optional<Long> getPid() {
        return Optional.ofNullable(this.pid);
    }
}