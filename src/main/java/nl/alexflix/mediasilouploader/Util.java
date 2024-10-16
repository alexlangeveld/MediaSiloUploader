package nl.alexflix.mediasilouploader;

import nl.alexflix.mediasilouploader.display.Splash;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    public static final List<String> httpLog = Collections.synchronizedList(new ArrayList<>());
    public static final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    public static final List<String> sucesses = Collections.synchronizedList(new ArrayList<>());
    public static final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
    public static List<String> allLogs = Collections.synchronizedList(new ArrayList<>());
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[27m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE_ON_RED = "\u001B[37;41m";
    public static final String ANSI_BLACK = "\u001B[0;30m";
    public static final String ANSI_WHITE = "\u001B[0;37m";

    // Bold
    public static final String ANSI_BOLD_BLACK = "\u001B[1;30m";
    public static final String ANSI_BOLD_RED = "\u001B[1;31m";
    public static final String ANSI_BOLD_GREEN = "\u001B[1;32m";
    public static final String ANSI_BOLD_YELLOW = "\u001B[1;33m";
    public static final String ANSI_BOLD_BLUE = "\u001B[1;34m";
    public static final String ANSI_BOLD_PURPLE = "\u001B[1;35m";
    public static final String ANSI_BOLD_CYAN = "\u001B[1;36m";
    public static final String ANSI_BOLD_WHITE = "\u001B[1;37m";

    // Underline
    public static final String ANSI_UNDERLINE_BLACK = "\u001B[4;30m";
    public static final String ANSI_UNDERLINE_RED = "\u001B[4;31m";
    public static final String ANSI_UNDERLINE_GREEN = "\u001B[4;32m";
    public static final String ANSI_UNDERLINE_YELLOW = "\u001B[4;33m";
    public static final String ANSI_UNDERLINE_BLUE = "\u001B[4;34m";
    public static final String ANSI_UNDERLINE_PURPLE = "\u001B[4;35m";
    public static final String ANSI_UNDERLINE_CYAN = "\u001B[4;36m";
    public static final String ANSI_UNDERLINE_WHITE = "\u001B[4;37m";

    // Background
    public static final String ANSI_BG_BLACK = "\u001B[40m";
    public static final String ANSI_BG_RED = "\u001B[41m";
    public static final String ANSI_BG_GREEN = "\u001B[42m";
    public static final String ANSI_BG_YELLOW = "\u001B[43m";
    public static final String ANSI_BG_BLUE = "\u001B[44m";
    public static final String ANSI_BG_PURPLE = "\u001B[45m";
    public static final String ANSI_BG_CYAN = "\u001B[46m";
    public static final String ANSI_BG_WHITE = "\u001B[47m";

    // High Intensity
    public static final String ANSI_HIGH_INTENSITY_BLACK = "\u001B[0;90m";
    public static final String ANSI_HIGH_INTENSITY_RED = "\u001B[0;91m";
    public static final String ANSI_HIGH_INTENSITY_GREEN = "\u001B[0;92m";
    public static final String ANSI_HIGH_INTENSITY_YELLOW = "\u001B[0;93m";
    public static final String ANSI_HIGH_INTENSITY_BLUE = "\u001B[0;94m";
    public static final String ANSI_HIGH_INTENSITY_PURPLE = "\u001B[0;95m";
    public static final String ANSI_HIGH_INTENSITY_CYAN = "\u001B[0;96m";
    public static final String ANSI_HIGH_INTENSITY_WHITE = "\u001B[0;97m";

    // Bold High Intensity
    public static final String ANSI_BOLD_HIGH_INTENSITY_BLACK = "\u001B[1;90m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_RED = "\u001B[1;91m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_GREEN = "\u001B[1;92m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_YELLOW = "\u001B[1;93m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_BLUE = "\u001B[1;94m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_PURPLE = "\u001B[1;95m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_CYAN = "\u001B[1;96m";
    public static final String ANSI_BOLD_HIGH_INTENSITY_WHITE = "\u001B[1;97m";

    // High Intensity Backgrounds
    public static final String ANSI_HIGH_INTENSITY_BG_BLACK = "\u001B[0;100m";
    public static final String ANSI_HIGH_INTENSITY_BG_RED = "\u001B[0;101m";
    public static final String ANSI_HIGH_INTENSITY_BG_GREEN = "\u001B[0;102m";
    public static final String ANSI_HIGH_INTENSITY_BG_YELLOW = "\u001B[0;103m";
    public static final String ANSI_HIGH_INTENSITY_BG_BLUE = "\u001B[0;104m";
    public static final String ANSI_HIGH_INTENSITY_BG_PURPLE = "\u001B[0;105m";
    public static final String ANSI_HIGH_INTENSITY_BG_CYAN = "\u001B[0;106m";
    public static final String ANSI_HIGH_INTENSITY_BG_WHITE = "\u001B[0;107m";

    // Reset
    public static final String ANSI_RESET = "\u001B[0m" + ANSI_BG_BLACK + ANSI_WHITE;


    public static void log(String message) {
        message = sanitize(message);
        if (Main.verbose()) System.out.println("[LOG] " + message);
        logs.add("[LOG]  " + message);
        allLogs.add("[LOG]  " + LocalDateTime.now() + " :  " + message);
        Splash.setLogText(message);
        Main.logger.add("[LOG]  " + message);
    }

    public static void log(int number) {
        if (Main.verbose()) System.out.println("[LOG] " + ANSI_BLUE + number + ANSI_RESET);
        logs.add("[LOG] " + ANSI_BLUE + number + ANSI_RESET);
        allLogs.add("[LOG]  " + LocalDateTime.now() + " :  " + number);
        Main.logger.add("[LOG]  " + number);
    }


    public static void http(@Nullable String message) {
        message = sanitize(message);
        if (Main.verbose()) System.out.println(ANSI_YELLOW + "[HTTP] " + ANSI_RESET + message);
        httpLog.add(ANSI_YELLOW + "[HTTP] " + ANSI_RESET + message);
        allLogs.add("[HTTP] " + LocalDateTime.now() + " :  " + message);
        Main.logger.add("[HTTP] " + message);
    }

    public static void s3log(@Nullable String message) {
        message = sanitize(message);
        if (Main.verbose()) System.out.println(ANSI_YELLOW + "[HTTP] " + ANSI_RESET + message);
        httpLog.add(ANSI_YELLOW + "[AWS3] " + ANSI_RESET + message);
        allLogs.add("[AWS3] " + LocalDateTime.now() + " :  " + message);
        Main.logger.add("[AWS3] " + message);
    }

    public static void err(String message) {
        message = sanitize(message);
        System.out.println(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        errors.add(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        logs.add(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        allLogs.add("[ERR]  " + LocalDateTime.now() + " :  " + message);
        Splash.setLogText("[ERR]  " + message);
        Main.logger.add("[ERR]  " + message);
    }

    public static void err(Exception e) {
        String message = sanitize(e.getMessage());
        System.out.println(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        errors.add(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        logs.add(ANSI_WHITE_ON_RED + "[ERR]" + ANSI_RESET + " " + ANSI_RED + message + ANSI_RESET);
        allLogs.add("[ERR]  " + LocalDateTime.now() + " :  " + message);
        for (StackTraceElement element : e.getStackTrace()) {
            allLogs.add("[ERR]  " + LocalDateTime.now() + " :  " + element.toString());
        }
        Main.logger.add("[ERR]  " + message);
        Main.logger.add(e);
        Splash.setLogText("[ERR]  " + message);
    }

    public static void success(String message) {
        message = sanitize(message);
        if (Main.verbose()) System.out.println(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        logs.add(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        sucesses.add(ANSI_GREEN + "[SUCCESS] " + ANSI_RESET + message);
        allLogs.add("[SUCC] " + LocalDateTime.now() + " :  " + message);
        Splash.setLogText(message);
        Main.logger.add("[SUCC] " + message);
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

        System.out.println();
        System.out.println("Alle logs:");

        synchronized (logs) {
            for (String log : logs) {
                System.out.println(log);
            }
        }
        System.out.flush();
        synchronized (httpLog) {
            System.out.flush();
            for (String log : httpLog) {
                System.out.println(log);
            }
        }
        System.out.flush();

        synchronized (sucesses) {
            for (String log : sucesses) {
                System.out.println(log);
            }
        }
        System.out.flush();

        synchronized (errors) {
            for (String log : errors) {
                System.out.println(log);
            }
        }
        System.out.flush();

        synchronized (exceptions) {
            for (Throwable e : exceptions) {
                System.out.println(ANSI_RED);
                e.printStackTrace();
                System.out.println(ANSI_RESET);
            }
        };
        System.out.flush();
    }


    private static final Pattern SECRET_PATTERN = Pattern.compile("(x-secret:)([\\w-]+)");
    private static final Pattern KEY_PATTERN = Pattern.compile("(x-key:)([\\w-]+)");

    private static String sanitize(String logMessage) {
        Matcher secretMatcher = SECRET_PATTERN.matcher(logMessage);
        logMessage = secretMatcher.replaceAll("$1[REDACTED]");

        Matcher keyMatcher = KEY_PATTERN.matcher(logMessage);
        logMessage = keyMatcher.replaceAll("$1[REDACTED]");

        return logMessage;
    }

    public static String getLogsHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML>");

        for (int i = allLogs.size() - 5; i < allLogs.size(); i++) {
            sb.append(allLogs.get(i));
            sb.append("<br>");
        }

        for (String error : errors) {
            String replaced_error = error
                    .replace(ANSI_WHITE_ON_RED, "")
                    .replace(ANSI_RESET, "")
                    .replace(ANSI_RED, "");

            sb.append("<font color=\"red\">").append(replaced_error).append("</font>");
            sb.append("<p>").append(replaced_error).append("</p>");
            sb.append("<br>");
        }



        sb.append("</HTML>");

        String rtn = sb.toString()
                .replace("[SUCC]", "<font color=\"green\">[SUCC]</font>")
                .replace("[ERR]", "<font color=\"red\">[ERR]</font>")
                .replace("[HTTP]", "<font color=\"yellow\">[HTTP]</font>");
        return rtn;

    }
}


