package nl.alexflix.mediasilouploader;

import nl.alexflix.mediasilouploader.display.Display;
import nl.alexflix.mediasilouploader.display.SimpleDisplay;
import nl.alexflix.mediasilouploader.local.Cleaner;
import nl.alexflix.mediasilouploader.local.Transcoder;
import nl.alexflix.mediasilouploader.local.Watchfolder;
import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.remote.email.Emailer;
import nl.alexflix.mediasilouploader.remote.mediasilo.Uploader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Scanner;

public class Main {

    private static Watchfolder watchfolder;
    public static Display display;
    private static Thread displayThread;
    private static Thread watchfolderThread;
    public static final List<Export> exports = new ArrayList<>();
    private static final LinkedBlockingQueue<Export> transcodeQueue = new LinkedBlockingQueue<>();
    private static Thread transcoderThread;
    private static final LinkedBlockingQueue<Export> uploadQueue = new LinkedBlockingQueue<>();
    private static Thread uploaderThread;
    private static final LinkedBlockingQueue<Export> emailQueue = new LinkedBlockingQueue<>();
    private static Thread emailerThread;
    private static final LinkedBlockingQueue<Export> doneQueue = new LinkedBlockingQueue<>();
    private static Thread cleanerThread;


    public static void main(String[] args) {
        System.out.println("Hello world!");

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--folder")) watchFolderPath = args[i + 1];
                if (args[i].equalsIgnoreCase("--ffmpeg")) ffmpegPath = args[i + 1];
                if (args[i].equalsIgnoreCase("--mediaInfo")) mediaInfoPath = args[i + 1];
                if (args[i].equalsIgnoreCase("--verbose")) verbose = true;
                if (args[i].equalsIgnoreCase("--apiKey")) mediaSiloAPIkey = args[i + 1];
                if (args[i].equalsIgnoreCase("--apiSecret")) mediaSiloAPIsecret = args[i + 1];
                if (args[i].equalsIgnoreCase("--project")) projectNaam = args[i + 1];
                if (args[i].equalsIgnoreCase("--emailTemplate")) emailTemplatePath = args[i + 1];
                if (args[i].equalsIgnoreCase("--logdir")) logFileDir = args[i + 1];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Util.err("Er is iets goed mis met je commando:");
            Util.err("Gebruik: --folder <folder> --ffmpeg <path> --mediaInfo <path> --verbose --apiKey <key> --apiSecret <secret>");
        }


        watchfolder = new Watchfolder(watchFolderPath, transcodeQueue, exports);
        watchfolderThread = new Thread(watchfolder);
        watchfolderThread.setName("WatchfolderThread");
        watchfolderThread.start();

        Transcoder transcoder = new Transcoder(transcodeQueue, uploadQueue, ffmpegPath);
        transcoderThread = new Thread(transcoder);
        transcoderThread.setName("TranscoderThread");
        transcoderThread.start();

        Uploader uploader = new Uploader(uploadQueue, emailQueue, mediaSiloAPIkey, mediaSiloAPIsecret);
        uploaderThread = new Thread(uploader);
        uploaderThread.setName("UploaderThread");
        uploaderThread.start();

        Emailer emailer = new Emailer(emailQueue, emailTemplatePath, doneQueue);
        emailerThread = new Thread(emailer);
        emailerThread.setName("EmailerThread");
        emailerThread.start();

        Cleaner cleaner = new Cleaner(doneQueue, watchfolder.getDonePath().toString());
        cleanerThread = new Thread(cleaner);
        cleanerThread.setName("CleanerThread");
        cleanerThread.start();

        display = new SimpleDisplay(exports);
        displayThread = new Thread(display);
        displayThread.setName("DisplayThread");
        displayThread.start();


        //waitForQuit(watchfolder, watchfolderThread, transcoderThread, uploaderThread, emailerThread);
        waitForQuit(watchfolderThread, transcoderThread, uploaderThread, emailerThread, displayThread);
        Util.log2file();
        Util.printAll();
    }

    private static void waitForQuit(Watchfolder watchfolder, Thread... threads) {
        Scanner scanner = new Scanner(System.in);
        while (!exit) {
            if (scanner.nextLine().equalsIgnoreCase("EXIT")) {
                Util.success("Afsluiten...");
                exit(watchfolder, threads);

            }
        }
        scanner.close();
    }

    private static void waitForQuit(Thread... threads) {
        for (Thread thread : threads) {
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    waitForQuit(threads);
                }
            }
        }
    }

    public static void exit() {
        exit(watchfolder, watchfolderThread, transcoderThread, uploaderThread, emailerThread, cleanerThread);
    }

    public static void exit(Watchfolder watchfolder,
                               Thread... threads) {
        try {

            Main.exit = true;

            watchfolder.stop();
            display.stop();
            Export exit = new Exit();
            exports.add(exit);
            transcodeQueue.put(exit);

            for (Thread thread : threads) {
                if (thread != null) thread.join();
            }

        } catch (InterruptedException e) {
            Util.err(e.getMessage());
            for (Thread thread : threads) {
                thread.interrupt();
            }
        }


    }

    private static boolean exit = false;
    private static boolean verbose = true;
    private static String watchFolderPath = "";
    private static String ffmpegPath = "ffmpeg";
    private static String mediaInfoPath = "mediainfo";
    private static String mediaSiloAPIkey = "";
    private static String mediaSiloAPIsecret = "";
    private static String projectNaam = "";
    private static String emailTemplatePath = "";
    public static String logFileDir;


    public static boolean verbose() {
        return verbose;
    }
    public static void verbose(boolean verbose) {
        Main.verbose = verbose;
    }


    public static String mediainfoPath() {
        return mediaInfoPath;
    }

    public static String ProjectNaam() {
        return projectNaam;
    }


    public static boolean queuesEmpty() {
        return Uploader.noThreadsRunning() && transcodeQueue.isEmpty() && uploadQueue.isEmpty() && emailQueue.isEmpty();
    }

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                exit();
            } catch (Exception e) {
                e.printStackTrace();
            }}));
    }
}