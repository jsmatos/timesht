package com.jsmatos.timesheets.storage;

import com.jsmatos.timesheets.model.LogEntry;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryRepository {
    private static final String HOME = System.getenv("HOME");

    public LogEntry save(LogEntry logEntry) {
        DateFormat taskTimeOfDayFormat = new SimpleDateFormat("HH:mm:ss");

        try (FileOutputStream fos = new FileOutputStream(getTodayFile(), true);
             PrintStream ps = new PrintStream(fos)) {
            String text = String.format("[%s] %s", taskTimeOfDayFormat.format(logEntry.getWhen()), logEntry.getWhat().replaceAll("\n", "%n"));
            int length = text.length();
            ps.print(length);
            ps.print(" ");
            ps.println(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return logEntry;
    }

    public List<LogEntry> findByFilter(String filter, boolean groupByTask, int lastN) {
        System.out.println(String.format("Repository.findBy (%s)", filter));
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minus(1, ChronoUnit.MONTHS);
        File currentMonthFolder = getMonthFolder(now);
        File previousMonthFolder = getMonthFolder(previousMonth);
        Stream<File> filenamesStream = Stream.concat(
                lastNDaysFilesFromFolder(currentMonthFolder, now, 7).stream(),
                lastNDaysFilesFromFolder(previousMonthFolder, now, 7).stream()
        );

        final List<LogEntry> result;
        final Stream<LogEntry> matchingEntries = filenamesStream
                .map(this::entriesFromFile)
                .flatMap(Collection::stream)
                .filter(le -> StringUtils.containsIgnoreCase(le.getWhat(), filter));
        if (groupByTask) {
            Map<String, List<LogEntry>> groupedByTask = matchingEntries
                    .collect(Collectors.groupingBy(LogEntry::getWhat));
            result = groupedByTask.values().stream().map(this::getLatest)
                    .sorted(Comparator.comparing(LogEntry::getWhen))
                    .collect(lastN(lastN));
        }else {
            result = matchingEntries
                    .sorted(Comparator.comparing(LogEntry::getWhen))
                    .collect(lastN(lastN));
        }
        System.out.println(String.format("Repository.findBy (%s) returning %d results", filter, result.size()));
        return result;
    }

    private LogEntry getLatest(List<LogEntry> list) {
        list.sort(Comparator.comparing(LogEntry::getWhen).reversed());
        return list.iterator().next();
    }

    public static <T> Collector<T, ?, List<T>> lastN(int n) {
        return Collector.<T, Deque<T>, List<T>>of(ArrayDeque::new, (acc, t) -> {
            if (acc.size() == n)
                acc.pollFirst();
            acc.add(t);
        }, (acc1, acc2) -> {
            while (acc2.size() < n && !acc1.isEmpty()) {
                acc2.addFirst(acc1.pollLast());
            }
            return acc2;
        }, ArrayList::new);
    }

    private List<File> lastNDaysFilesFromFolder(File folder, LocalDate startDate, int untilDaysBefore) {
        if (folder.exists()) {
            String[] filenames = folder.list(new PreviousDaysFilter(startDate, untilDaysBefore));
            if (filenames != null) {
                List<File> files = new ArrayList<>();
                for (String file : filenames) {
                    files.add(Paths.get(folder.getPath(), file).toFile());
                }
                return files;
            }
        }
        return Collections.emptyList();
    }

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

    private List<LogEntry> entriesFromFile(File file) {
        Path path = file.toPath();
        try {
            return Files.lines(path)
                    .filter(StringUtils::isNotBlank)
                    .map(line -> fromLine(line, file.getPath()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static void main(String[] args) throws IOException {
        FileInputStream f = new FileInputStream("/home/jorge/logs/timesheet/2020/JANUARY/10");
        f.close();
        
    }

    private LogEntry fromLine(String line, String fileName) {
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
