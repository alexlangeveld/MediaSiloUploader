package nl.alexflix.mediasilouploader;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Util {
    public volatile static List<String> logs = new ArrayList<>();
    public volatile static List<String> httpLog = new ArrayList<>();
    public volatile static List<String> errors = new ArrayList<>();
    public volatile static List<String> sucesses = new ArrayList<>();
    public volatile static List<Throwable> exceptions = new ArrayList<>();
    public volatile static List<String> allLogs = new ArrayList<>();
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_WHITE_ON_RED = "\u001B[37;41m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static void log(String message) {
        if (Main.verbose()) System.out.println("[LOG] " + message);
        logs.add("[LOG] " + message);
        allLogs.add("[LOG] " + LocalDateTime.now() + " :  " + message);
    }

    public static void log(int number) {
        if (Main.verbose()) System.out.println("[LOG] " + ANSI_BLUE + number + ANSI_RESET);
        logs.add("[LOG] " + ANSI_BLUE + number + ANSI_RESET);
        allLogs.add("[LOG]  " + LocalDateTime.now() + " :  " + number);
    }


    public static void http(@Nullable String message) {
        if (Main.verbose()) System.out.println(ANSI_YELLOW + "[HTTP] " + ANSI_RESET + message);
        httpLog.add(ANSI_YELLOW + "[HTTP] " + ANSI_RESET + message);
        allLogs.add("[HTTP] " + LocalDateTime.now() + " :  " + message);
    }

    public static void err(String message) {
        System.out.println(ANSI_WHITE_ON_RED + "[ERR]"  + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        errors.add(ANSI_WHITE_ON_RED + "[ERR]"  + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        logs.add(ANSI_WHITE_ON_RED + "[ERR]"  + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        allLogs.add("[ERR]  " + LocalDateTime.now() + " :  " + message);
    }

    public static void success(String message) {
        if (Main.verbose()) System.out.println(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        logs.add(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        sucesses.add(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        allLogs.add("[SUCC] " + LocalDateTime.now() + " :  " + message);
    }



    public static String datum() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return formatter.format(java.time.LocalDateTime.now());
    }
    public static String tijd() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm");
        return formatter.format(java.time.LocalDateTime.now());
    }

    public static Throwable[] getExceptions() {
        return exceptions.toArray(new Throwable[0]);
    }
    public static String[] getErrors() {
        return errors.toArray(new String[0]);
    }

    public static void printAll() {
        System.out.flush();
        for (String log : logs) {
            System.out.println(log);
        }
        System.out.flush();
        for (String log : httpLog) {
            System.out.println(log);
        }
        System.out.flush();
        for (String log : sucesses) {
            System.out.println(log);
        }
        System.out.flush();
        for (String log : errors) {
            System.out.println(log);
        }
        System.out.flush();

        for (Throwable e : exceptions) {
            System.out.println(ANSI_RED);
            e.printStackTrace();
            System.out.println(ANSI_RESET);
        }
        System.out.flush();
    }

    public static boolean log2file() {
        if (Main.logFileDir == null) return false;
        String logFilePath = Main.logFileDir + File.separator + "MSuploaderLOG_" + LocalDate.now() + ".log";
        try (FileWriter writer = new FileWriter(new File(logFilePath), true)){
            for (String log : allLogs) {
                writer.write(log);
                writer.write("\n");
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static void log2file(String path) {
        File logFile = new File(path);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileWriter writer = new FileWriter(logFile)) {
            for (String log : logs) {
                writer.write(log + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


