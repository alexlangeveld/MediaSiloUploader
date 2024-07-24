package nl.alexflix.mediasilouploader.display;

import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Incoming;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class SimpleDisplay implements Display {
    private List<Export> exports;
    private List<Export> incomings;
    public boolean running = true;
    public String tempStatus = "";

    private final String[] logo = {
            "|\\/| _  _|o _.(_ o| _  | |._ | _  _. _| _ ._ ",
            "|  |(/_(_||(_|__)||(_) |_||_)|(_)(_|(_|(/_|   ",
            "                          |                   "
    };

    public SimpleDisplay(List<Export> exports) {
        this.exports = exports;
        this.incomings = new ArrayList<>();
    }

    @Override
    public void run() {
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    Main.exit();
                }
            }
        });
        inputThread.setDaemon(true); // Ensure the input thread doesn't block JVM shutdown
        inputThread.start();

        Main.verbose(false);
        while (running) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignore) { }
            if (!running) break;
            String output = render();
            clearScreen();
            System.out.print(output);

            for (String error : Util.getErrors()) {
                System.out.println(error);
            }



        }
        Main.verbose(true);
    }

    private String render() {
        StringBuilder sb = new StringBuilder();

        sb.append("\u001B[1m" + "\u001B[34m");
        sb.append(logo[0] + "\n");
        sb.append(logo[1] + "\n");
        sb.append(logo[2] + "\n");
        sb.append("\u001B[0m");


        sb.append("\u001B[4m");
        sb.append(
                "EXPORT                                   | Transcoden | Uploaden | Transcoden | Verzonden | Deleten    "
        );
        sb.append("\u001B[0m" + "\n");
        for (Export export : exports) {
            String timeToLive = " | --:--:--";
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime timeOfDeath = export.getTimeOfDeath();
                Duration duration = Duration.between(now, timeOfDeath);
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;
                timeToLive = String.format(" | %02d:%02d:%02d", hours, minutes, seconds);
            } catch (Exception e) {
                timeToLive = " | --:--:--";
            }


            sb.append(
                    String.format("%40s |       %3d%% |     %3d%% |       %3d%% |",
                            export.toSubString(40),
                            export.getLocalTranscodeProgress(),
                            export.getUploadProgress(),
                            export.getRemoteTranscodeProgress()
                    )
                            + (export.isEmailSent() ? (Util.ANSI_GREEN + "    Ja    " + Util.ANSI_RESET)  : "    Nee   ")
                            + timeToLive
            );

            sb.append("\n");
        }

        if (!incomings.isEmpty()) {
            Iterator<Export> iterator = incomings.iterator();
            while (iterator.hasNext()) {
                Export incoming = iterator.next();
                String timeToLive = "  | --:--:--";
                sb.append(
                        String.format("%40s |        %3d |      %3d |        %3d |",
                                incoming.toSubString(40),
                                incoming.getLocalTranscodeProgress(),
                                incoming.getUploadProgress(),
                                incoming.getRemoteTranscodeProgress()
                        )
                                + (incoming.isEmailSent() ? "    Ja    " : "    Nee   ")
                                + timeToLive
                );
                iterator.remove();
            }
        }



        sb.append("\n");

        sb.append(tempStatus);
        sb.append("\n");
        sb.append("\u001B[0m");

        return sb.toString();
    }



    @NotNull
    private static Thread getThread() {
        Thread inputThread = new Thread(() -> {
            final int Q_THRESHOLD = 100;
            try (Scanner scanner = new Scanner(System.in)) {
                int qCount = 0;
                while (true) {
                    if (scanner.hasNext()) {
                        String input = scanner.next();
                        for (char c : input.toCharArray()) {
                            if (c == 'Q' || c == 'q') {
                                qCount++;
                                if (qCount == Q_THRESHOLD) {
                                    Main.exit();
                                }
                            } else {
                                //qCount = 0;
                            }
                        }
                    }
                }
            }
        });
        inputThread.setDaemon(true); // Ensure the input thread doesn't block JVM shutdown
        return inputThread;
    }

    private void clearScreen() {
        try {
            //System.out.println("Clearing Screen...");
            var clearCommand = System.getProperty("os.name").contains("Windows")
                    ? new ProcessBuilder("cmd", "/c", "cls")
                    : new ProcessBuilder("clear");
            clearCommand.inheritIO().start().waitFor();
        }
        catch (IOException | InterruptedException e) {}
    }


    @Override
    public void stop() {
        running = false;
    }


    public void addIncoming(Incoming incoming) {
        if (incomings.contains(incoming)) return;
        incomings.add(incoming);
    }
}
