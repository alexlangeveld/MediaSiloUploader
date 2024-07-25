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
    public boolean running = true;
    public String tempStatus = "";

    private static final String[] logo1 = {
            "|\\/| _  _|o _.(_ o| _  | |._ | _  _. _| _ ._ ",
            "|  |(/_(_||(_|__)||(_) |_||_)|(_)(_|(_|(/_|   ",
            "                          |                   "
    };

    private static final String[] logo2 = {
            "=======================================================================================================================",
            "=  =====  ============  ==============      =======  =========  ====  =========  ===================  =================",
            "=   ===   ============  =============  ====  ======  =========  ====  =========  ===================  =================",
            "=  =   =  ============  =============  ====  ======  =========  ====  =========  ===================  =================",
            "=  == ==  ===   ======  ==  ===   ====  =======  ==  ===   ===  ====  ==    ===  ===   ====   ======  ===   ===  =   ==",
            "=  =====  ==  =  ===    ======  =  =====  =========  ==     ==  ====  ==  =  ==  ==     ==  =  ===    ==  =  ==    =  =",
            "=  =====  ==     ==  =  ==  =====  =======  ===  ==  ==  =  ==  ====  ==  =  ==  ==  =  =====  ==  =  ==     ==  ======",
            "=  =====  ==  =====  =  ==  ===    ==  ====  ==  ==  ==  =  ==  ====  ==    ===  ==  =  ===    ==  =  ==  =====  ======",
            "=  =====  ==  =  ==  =  ==  ==  =  ==  ====  ==  ==  ==  =  ==   ==   ==  =====  ==  =  ==  =  ==  =  ==  =  ==  ======",
            "=  =====  ===   ====    ==  ===    ===      ===  ==  ===   ====      ===  =====  ===   ====    ===    ===   ===  ======",
            "======================================================================================================================="
    };

    private static final String[] logo3 = {
        "    __  ___         _         _____   __      __  __      __                __         ",
        "   /  |/  /__  ____/ (_)___ _/ ___/(_) /___  / / / /___  / /___  ____ _____/ /__  _____",
        "  / /|_/ / _ \\/ __  / / __ `/\\__ \\/ / / __ \\/ / / / __ \\/ / __ \\/ __ `/ __  / _ \\/ ___/",
        " / /  / /  __/ /_/ / / /_/ /___/ / / / /_/ / /_/ / /_/ / / /_/ / /_/ / /_/ /  __/ /    ",
        "/_/  /_/\\___/\\____/_/\\____//____/_/_/\\____/\\____/ .___/_/\\____/\\____/\\____/\\___/_/     ",
        "                                               /_/                               ",
        "                                                                                 "
    };

    private static final String[] logo = logo3;

    public SimpleDisplay(List<Export> exports) {
        this.exports = exports;
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
        animate();
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

        sb.append(Util.ANSI_RESET);

        sb.append(Util.ANSI_BOLD_HIGH_INTENSITY_BLUE);
        for (String line : logo) {
            sb.append(line + "\n");
        }
        sb.append(Util.ANSI_RESET);


        sb.append("\u001B[4m");
        sb.append(
                "EXPORT                                   | Transcoden | Uploaden | Transcoden | Verzonden | Deleten    "
        );
        sb.append("\u001B[0m" + "\n");
        for (Export export : exports) {
            String timeToLive;
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
                    String.format("%s |       %3d%% |     %3d%% |       %3d%% |",
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

        Incoming[] incomings = Main.getWatchfolder().getAll();


        for (Incoming incoming : incomings) {
            if (incoming.isHidden()) continue;
            sb.append(
                    String.format("%40s |       %3d%% |     %3d%% |       %3d%% |",
                            incoming.toSubString(40),
                            incoming.getLocalTranscodeProgress(),
                            incoming.getUploadProgress(),
                            incoming.getRemoteTranscodeProgress()
                    )
            );
            sb.append("\n");
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

    private static void clearScreen() {
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

    private static void animate() {
        clearScreen();
        final int sleepTime = 10;
        String[] ansiColors = {
                Util.ANSI_BOLD_HIGH_INTENSITY_YELLOW,
                Util.ANSI_BOLD_HIGH_INTENSITY_BLUE,
                Util.ANSI_BOLD_HIGH_INTENSITY_GREEN,
                Util.ANSI_BOLD_HIGH_INTENSITY_CYAN,
                Util.ANSI_BOLD_HIGH_INTENSITY_PURPLE
        };


        System.out.println(Util.ANSI_BOLD_HIGH_INTENSITY_BLUE);
        for (int i = 0; i < logo.length; i++) {
            char[] chars = logo[i].toCharArray();
            for (int j = 0 ; j < chars.length; j++) {
                System.out.print(chars[j]);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            }
            System.out.println();
        }
        System.out.println(Util.ANSI_RESET);
        clearScreen();

        for (int i = 0; i < ansiColors.length; i++) {
            System.out.println(ansiColors[i]);
            for (int j = 0; j < logo.length; j++) {
                System.out.println(logo[j]);
            }

            System.out.println();
            try {
                Thread.sleep(sleepTime * 40);
            } catch (InterruptedException ignored) {
            }
            clearScreen();
        }
        clearScreen();
        System.out.println(Util.ANSI_RESET);
        System.out.println("Loading...");
    }


}
