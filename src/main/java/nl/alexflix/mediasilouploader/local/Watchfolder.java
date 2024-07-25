package nl.alexflix.mediasilouploader.local;



import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Incoming;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.Project;

import java.io.File;
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
    private IncomingQueue incomingQueue;

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
        this.incomingQueue = new IncomingQueue();
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
                            incomingQueue.remove(file);
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
                            incomingQueue.add(file);
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
        Util.success("Watchfolder gestopt");


    }


    // TO DO, implement mediainfo logic
    private boolean isFileReady(File file) {
        boolean isMXF = file.getName().substring(file.getName().lastIndexOf('.')).equalsIgnoreCase(".MXF");
        if (!isMXF) return false;
        return MediaInfo.isClosed(file.getPath());
    }

    public synchronized Incoming[] getAll() {
        return incomingQueue.getAll();
    }

    public File getDonePath() {
        return donePath;
    }


    public void stop() {
        exit = true;
    }


}

class IncomingQueue {
    private volatile ArrayList<Incoming> queue;

    IncomingQueue() {
        this.queue = new ArrayList<>();
    }


    void add(File file) {
        Incoming incoming = new Incoming(file);
        if (queue.contains(incoming)) return;
        queue.add(incoming);
    }

    void remove(File file) {
        Incoming incoming = new Incoming(file);
        if (!queue.contains(incoming)) return;
        queue.remove(incoming);
    }
    Incoming[] getAll() {
        return queue.toArray(new Incoming[0]);
    }

}
