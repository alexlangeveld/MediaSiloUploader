package nl.alexflix.mediasilouploader.local;



import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.display.Display;
import nl.alexflix.mediasilouploader.display.SimpleDisplay;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Incoming;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Watchfolder implements Runnable{
    public static boolean exit = false;
    private final File path;
    File inProgresPath;
    File donePath;
    List<Export> exports;
    LinkedBlockingQueue<Export> transCodeQueue;
    //Timer timer;

    public Watchfolder(String path, LinkedBlockingQueue<Export> transCodeQueue, List<Export> exports) {
        this.transCodeQueue = transCodeQueue;
        this.exports = exports;
        this.path = new File(path);
        this.inProgresPath = new File(path + File.separator + ".verwerken" + File.separator);
        this.donePath = new File(path + File.separator + ".done");
        if (!this.path.isDirectory()) throw new RuntimeException("Pad " + path + " bestaat niet!");
        if (!this.inProgresPath.isDirectory()) inProgresPath.mkdir();
        if (!this.donePath.isDirectory()) donePath.mkdir();
        //timer = new Timer();
    }

    @Override
    public void run() {
        Util.success("Watchfolder gestart");
        while (!exit) {
            long startTime = System.currentTimeMillis();
            try {
                File[] files = path.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && isFileReady(file)) {
                            File newFile = new File(inProgresPath.getPath() + File.separator + file.getName());
                            if (file.renameTo(newFile)) {
                                Util.success("Bestand verplaatst naar " + newFile.getPath());
                                Export export = new Export(newFile);
                                exports.add(export);
                                transCodeQueue.put(export);
                            } else {
                                Util.err("Kon bestand niet verplaatsen: " + file.getName());
                            }
                        } else {
                            if (Main.display instanceof SimpleDisplay display) {
                                // display.addIncoming(new Incoming(file));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Util.err("Watchfolder: " + e.getMessage());
            }
            try {
                Thread.sleep(Math.max(0, 1000 - (System.currentTimeMillis() - startTime)));
            } catch (InterruptedException e) {
                Util.err("Watchfolder: " + e.getMessage());
            }
        }
        Util.success("Watchfolder gestopt");


    }


    // TO DO, implement mediainfo logic
    private boolean isFileReady(File file) {
        boolean isMXF = file.getName().substring(file.getName().lastIndexOf('.')).equalsIgnoreCase(".MXF");
        if (!isMXF) return false;
        return MediaInfo.isClosed(file.getPath());
    }

    public File getDonePath() {
        return donePath;
    }


    public void stop() {
        exit = true;
    }


}
