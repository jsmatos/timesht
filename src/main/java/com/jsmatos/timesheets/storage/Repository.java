package com.jsmatos.timesheets.storage;

import com.jsmatos.timesheets.model.LogEntry;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Repository {
    private static final String HOME = System.getenv("HOME");

    private File getTodayFile() {
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        File file = getMonthFolder(now);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(String.format("Path %s already exists and is not a directory", file.getPath()));
            }
        } else {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new IllegalStateException(String.format("Unable to create directory %s", file.getPath()));
            }
        }

        return Paths.get(file.getPath(), String.valueOf(dayOfMonth)).toFile();
    }

    private File getMonthFolder(LocalDate date) {
        int year = date.getYear();
        Month month = date.getMonth();
        Path path = Paths.get(HOME, "logs", "timesheet", String.valueOf(year), month.toString());
        return path.toFile();
    }

    public LogEntry save(LogEntry logEntry) {
        DateFormat taskTimeOfDayFormat = new SimpleDateFormat("HH:mm:ss");

        try (FileOutputStream fos = new FileOutputStream(getTodayFile(), true);
             PrintStream ps = new PrintStream(fos)) {
            ps.println(String.format("[%s] %s", taskTimeOfDayFormat.format(logEntry.getWhen()), logEntry.getWhat().replaceAll("\n", "%n")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return logEntry;
    }

    public List<LogEntry> findBy(String filter) {
        System.out.println(String.format("Repository.findBy (%s)", filter));
        LocalDate now = LocalDate.now();
        File monthFolder = getMonthFolder(now);
        if (!monthFolder.exists()) {
            return Collections.emptyList();
        }
        String[] fileNames = monthFolder.list(new PreviousDaysFilter(now, 7));
        if (fileNames == null) {
            return Collections.emptyList();
        }
        List<LogEntry> collect = Arrays.stream(fileNames)
                .map(fileName -> Paths.get(monthFolder.getPath(), fileName).toFile())
                .map(this::fromFile)
                .flatMap(Collection::stream)
                .filter(le -> StringUtils.containsIgnoreCase(le.getWhat(), filter))
                .sorted(Comparator.comparing(LogEntry::getWhen))
                .collect(Collectors.toList());
        System.out.println(String.format("Repository.findBy (%s) returning %d results", filter, collect.size()));
        return collect;
    }

    private Collection<LogEntry> fromFile(File file) {
        List<LogEntry> result = new ArrayList<>();
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line = br.readLine();
            while (line != null) {
                if (StringUtils.isNotBlank(line)) {
                    LogEntry logEntry = fromLine(line, file.getPath());
                    result.add(logEntry);
                }
                line = br.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    LogEntry fromLine(String line, String fileName) {
        String[] filenameParts = fileName.split(File.separator);
        String day = filenameParts[filenameParts.length - 1];
        String month = filenameParts[filenameParts.length - 2];
        String year = filenameParts[filenameParts.length - 3];

        String[] timeParts = line.substring(1, 9).split(":");


        LocalDateTime localDateTime = LocalDateTime.of(
                Integer.parseInt(year),
                Month.valueOf(month).getValue(),
                Integer.parseInt(day),
                Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1]),
                Integer.parseInt(timeParts[2])
        );

        String whatString = line.substring(11);
        whatString = String.format(whatString, "");
        Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return new LogEntry(date, whatString);
    }

    private static final class PreviousDaysFilter implements FilenameFilter {
        private final long minus;

        private PreviousDaysFilter(LocalDate from, int untilDaysBefore) {
            minus = from.minus(untilDaysBefore, ChronoUnit.DAYS).toEpochDay();
        }

        @Override
        public boolean accept(File file, String name) {
            return file.lastModified() >= minus;
        }
    }
}
