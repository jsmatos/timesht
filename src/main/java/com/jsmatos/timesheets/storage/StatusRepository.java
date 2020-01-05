package com.jsmatos.timesheets.storage;

import com.jsmatos.timesheets.gui.Alert;
import com.jsmatos.timesheets.model.Status;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class StatusRepository {
    private static final String HOME = System.getenv("HOME");
    public static final String PID = "pid";
    public static final String COMMAND_SERVER_PORT = "command.server.port";

    public Status getStatus() {
        File file = getFile();
        if (file.exists()) {
            Properties properties = new Properties();
            try (InputStream inputStream = new FileInputStream(file)) {
                properties.load(inputStream);
                long pid = Long.parseLong(properties.getProperty(PID));
                int port = Integer.parseInt(properties.getProperty(COMMAND_SERVER_PORT));
                return Status.runningOnPort(pid,port);
            } catch (IOException e) {
                Alert.warn("Timeshts", String.format("Exception loading status from file %s", file.getPath()));
                e.printStackTrace();
            }
        } else {
            return Status.NOT_RUNNING;
        }
        //ToDo:
        throw new RuntimeException("Remove me");
    }


    private File getFile() {
        Path pathToDir = Paths.get(HOME, "logs", "timesheet");
        File dir = pathToDir.toFile();
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();
            if (!mkdirs) {
                throw new IllegalStateException(String.format("Unable to create directory %s", dir.getPath()));
            }
        }
        return Paths.get(dir.getPath(), "status.properties").toFile();
    }

    public void setStatus(int commandServerPort) {
        Properties properties = new Properties();
        properties.setProperty(PID, String.valueOf(getCurrentPid()));
        properties.setProperty(COMMAND_SERVER_PORT, String.valueOf(commandServerPort));
        File file = getFile();
        file.deleteOnExit();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            properties.store(outputStream, "Don't change this file");
            System.out.println("status stored");
        } catch (IOException e) {
            Alert.warn("Timeshts", "Unable to save process status");
            e.printStackTrace();
        }
    }

    private long getCurrentPid(){
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        // Get name representing the running Java virtual machine.
        // It returns something like 6460@AURORA. Where the value
        // before the @ symbol is the PID.
        String jvmName = bean.getName();
        System.out.println("Name = " + jvmName);
        return Long.parseLong(jvmName.split("@")[0]);
    }

}
