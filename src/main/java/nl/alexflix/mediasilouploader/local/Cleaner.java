package nl.alexflix.mediasilouploader.local;

import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Cleaner implements Runnable {
    private boolean running = true;
    private final LinkedBlockingQueue<Export> doneQueue;
    private final List<cleanerThread> cleanerThreads = new ArrayList<>();
    private final File doneMap;

    public Cleaner(LinkedBlockingQueue<Export> doneQueue, String donePath) {
        this.doneQueue = doneQueue;
        this.doneMap = new File(donePath);
        if (!this.doneMap.isDirectory()) doneMap.mkdir();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Export export = doneQueue.take();
                if (export instanceof Exit) {
                    for (cleanerThread cleanerThread : cleanerThreads) {
                        cleanerThread.deleteNow();
                    }
                    running = false;
                    break;
                }
                export.moveFiles(this.doneMap.getPath());
                cleanerThread cleanerThread = new cleanerThread(export);
                cleanerThreads.add(cleanerThread);
                cleanerThread.start();
            } catch (InterruptedException e) {
                Util.err("Cleaner interrupt: " + e.getMessage());
            } catch (IOException e) {
                Util.err("Cleaner IOex: " + e.getMessage());
            }
        }
    }

}

class cleanerThread extends Thread {
    boolean running = true;
    private Export export;
    private boolean deleteImmediately = false;
    private LocalDateTime init;
    private LocalDateTime deletaAt;

    public cleanerThread(Export export) {
        this.export = export;
        init = LocalDateTime.now();
        deletaAt = init.plusMinutes(60);
        export.setTimeOfDeath(deletaAt);
    }


    @Override
    public void run() {
        while (running) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(deletaAt) || deleteImmediately) {
                deleteFiles();
                running = false;
                Main.exports.remove(export);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) { }
        }


    }

    private void deleteFiles() {
        try {
            if (export.getInputFile() != null) {
                Path inputFilePath = export.getInputFile().toPath();
                if (Files.exists(inputFilePath)) {
                    Files.delete(inputFilePath);
                    Util.log("Bestand verwijderd: " + inputFilePath);
                } else {
                    Util.err("Niet gevonden: " + inputFilePath);
                }
            } else Util.err(export.toString() + ": InputFile is null: " + export.getInputFile().getName());


            if (export.OutputFile() != null) {
                Path outputFilePath = export.OutputFile().toPath();
                if (Files.exists(outputFilePath)) {
                    Files.delete(outputFilePath);
                    Util.log("Bestand verwijderd: " + outputFilePath);
                } else {
                    Util.log("Niet gevonden: " + outputFilePath);
                }
            } else Util.err(export.toString() + ": OutputFile is null: " + export.OutputFile().getName());

        } catch (IOException e) {
            Util.err("CleanerThread IOex: " + e.getMessage());
            Util.exceptions.add(e);
        }
    }

    void deleteNow() {
        deleteImmediately = true;
    }

}
