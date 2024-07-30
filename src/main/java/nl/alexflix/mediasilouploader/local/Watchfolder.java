package nl.alexflix.mediasilouploader.local;



import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Incoming;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Watchfolder implements Runnable{
    private final long sleepTime = 1000;
    public static boolean exit = false;
    private final File path;
    private final Project project;
    File inProgresPath;
    File donePath;
    List<Export> exports;
    LinkedBlockingQueue<Export> transCodeQueue;
    //Timer timer;


    public Watchfolder(String path, LinkedBlockingQueue<Export> transCodeQueue, List<Export> exports, Project project) {
        this.transCodeQueue = transCodeQueue;
        this.exports = exports;
        this.project = project;
        this.path = new File(path + File.separator + this.project.getName() + File.separator);
        this.inProgresPath = new File(path + File.separator + ".verwerken" + File.separator);
        this.donePath = new File(path + File.separator + ".done");
        if (!new File(path).isDirectory()) {
            exports.add(new Exit());
            throw new RuntimeException("Pad " + path + " bestaat niet!");
        }
        if (!this.path.isDirectory()) this.path.mkdir();
        if (!this.inProgresPath.isDirectory()) inProgresPath.mkdir();
        if (!this.donePath.isDirectory()) donePath.mkdir();

        makeHidden(donePath);
        makeHidden(inProgresPath);

    }

    @Override
    public void run() {
        Util.success(Thread.currentThread().getName() + " gestart");
        while (!exit) {
            long startTime = System.currentTimeMillis();
            try {
                File[] files = path.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && isFileReady(file)) {
                            Main.removeIncoming(file);
                            File newFile = new File(inProgresPath.getPath() + File.separator + file.getName());
                            if (file.renameTo(newFile)) {
                                Util.success("Bestand verplaatst naar " + newFile.getPath());
                                Export export = new Export(newFile);
                                export.setProject(project);
                                exports.add(export);
                                transCodeQueue.put(export);
                            } else {
                                Util.log("Kon bestand niet verplaatsen: " + file.getName());
                            }
                        } else if (file.isFile() && !isFileReady(file)) {
                            Main.addIncoming(file);
                        }
                    }
                }
            } catch (Exception e) {
                Util.err("Watchfolder: " + e.getMessage());
            }
            try {
                Thread.sleep(Math.max(0, sleepTime - (System.currentTimeMillis() - startTime)));
            } catch (InterruptedException e) {
                Util.err("Watchfolder: " + e.getMessage());
            }
        }
        Util.success(Thread.currentThread().getName() + " gestopt");


    }


    // TO DO, implement mediainfo logic
    private boolean isFileReady(File file) {
        boolean isMXF = file.getName().substring(file.getName().lastIndexOf('.')).equalsIgnoreCase(".MXF");
        if (!isMXF) return false;
        return MediaInfo.isClosed(file.getPath());
    }

//    public synchronized Incoming[] getAll() {
//        return incomingQueue.getAll();
//    }

    public File getDonePath() {
        return donePath;
    }


    public void stop() {
        exit = true;
    }


    private void makeHidden(File file) {
        if (!(System.getProperty("os.name").toLowerCase().contains("win"))) return;
        if (file.exists()) {
            try {
                ProcessBuilder pb = new ProcessBuilder("attrib", "+H", file.getAbsolutePath());
                pb.start().waitFor();
            } catch (IOException | InterruptedException e) {
                Util.err("Watchfolder::makeHidden: " + e.getMessage());;
            }
        }
    }

}


