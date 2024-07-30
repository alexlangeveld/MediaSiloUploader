package nl.alexflix.mediasilouploader;

import nl.alexflix.mediasilouploader.display.Display;
import nl.alexflix.mediasilouploader.display.SimpleDisplay;
import nl.alexflix.mediasilouploader.local.Cleaner;
import nl.alexflix.mediasilouploader.local.Transcoder;
import nl.alexflix.mediasilouploader.local.Watchfolder;
import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.local.types.Incoming;
import nl.alexflix.mediasilouploader.remote.email.Emailer;
import nl.alexflix.mediasilouploader.remote.mediasilo.UploadThread;
import nl.alexflix.mediasilouploader.remote.mediasilo.Uploader;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static Logger logger;
    private static Thread loggerThread;
    private static Project[] projects;
    private static Project defaultProject;
    private static Watchfolder[] watchfolders;
    private static volatile ArrayList<Incoming> incomings;
    public static Display display;
    private static Thread displayThread;
    private static Thread[] watchfolderThreads;
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
        System.out.println("MediaSiloUploader v0.2.3");

        Map<String, String> envVars = System.getenv();
        if (envVars.containsKey("APIkey") && envVars.containsKey("APIsecret")) {
            mediaSiloAPIkey = envVars.get("APIkey");
            mediaSiloAPIsecret = envVars.get("APIsecret");
        } else Util.err("APIkey en/of APIsecret niet gevonden in de Environment Variables");

        if (envVars.containsKey("emailTemplate")) emailTemplatePath = envVars.get("emailTemplate");


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



        logger = new Logger(logFileDir);
        loggerThread = new Thread(logger);
        loggerThread.setName("LoggerThread");
        loggerThread.setPriority(Thread.MIN_PRIORITY);
        loggerThread.start();

        //Alle projecten ophalen
        try {
            Util.log("Projecten ophalen...");
            projects = UploadThread.getAllProjects(mediaSiloAPIkey, mediaSiloAPIsecret).toArray(new Project[0]);
            Util.success(projects.length + " projecten succesvol opgehaald");
            for (int i = 0; i < projects.length; i++) {
                Util.log("(" + (i + 1) + "/" + projects.length + ") " + projects[i].getName());
                if (projects[i].getName().equalsIgnoreCase(projectNaam)) defaultProject = projects[i];
            }
        } catch (IOException e) {
            Util.err("Kan projecten niet ophalen: " + e.getMessage());
            Util.exceptions.add(e);
        }

        watchfolders = new Watchfolder[projects.length];
        watchfolderThreads = new Thread[watchfolders.length];

        for (int i = 0; i < watchfolders.length; i++) {
            watchfolders[i] = new Watchfolder(watchFolderPath, transcodeQueue, exports, projects[i]);
            watchfolderThreads[i] = new Thread(watchfolders[i]);
            watchfolderThreads[i].setName("Watchfolder" + (i + 1) + " voor " + projects[i].getName());
            watchfolderThreads[i].start();
        }

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

        Cleaner cleaner = new Cleaner(doneQueue, (watchFolderPath + File.separator + ".done"));
        cleanerThread = new Thread(cleaner);
        cleanerThread.setName("CleanerThread");
        cleanerThread.start();

        display = new SimpleDisplay(exports);
        displayThread = new Thread(display);
        displayThread.setName("DisplayThread");
        displayThread.start();


        //waitForQuit(watchfolder, watchfolderThread, transcoderThread, uploaderThread, emailerThread);
        List<Thread> allThreads = new ArrayList<>();
        allThreads.add(transcoderThread);
        allThreads.add(uploaderThread);
        allThreads.add(emailerThread);
        allThreads.add(cleanerThread);
        allThreads.add(displayThread);
        allThreads.addAll(Arrays.asList(watchfolderThreads));
        waitForQuit(allThreads.toArray(new Thread[0]));
        Util.printAll();

        logger.stop();
        try {
            loggerThread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
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
        exit(watchfolders, transcoderThread, uploaderThread, emailerThread, cleanerThread);
    }

    public static void exit(Watchfolder[] watchfolders, Thread... threads) {
        try {

            Main.exit = true;
            for (Watchfolder watchfolder : watchfolders) {
                watchfolder.stop();
            }
            Export exit = new Exit();
            exports.add(exit);
            transcodeQueue.put(exit);
            display.stop();

            for (Thread thread : threads) {
                if (thread != null) thread.join();
            }

        } catch (InterruptedException e) {
            Util.err(e.getMessage());
            for (Thread thread : threads) {
                thread.interrupt();
            }
        } catch (NullPointerException e) {
            Util.err(e.getMessage());
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

    public static Incoming[] getAllIncoming() {
        if (incomings == null) return new Incoming[0];
        return incomings.toArray(new Incoming[0]);
    }
    public static void addIncoming(File incomingFile) {
        if (incomings == null) incomings = new ArrayList<>();
        Incoming incoming = new Incoming(incomingFile);
        if (incomings.contains(incoming)) return;
        incomings.add(incoming);
    }
    public static void removeIncoming(File file) {
        if (incomings == null) return;
        Incoming incoming = new Incoming(file);
        if (!incomings.contains(incoming)) return;
        incomings.remove(incoming);
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