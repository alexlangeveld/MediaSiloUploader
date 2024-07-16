package nl.alexflix.mediasilouploader.remote.mediasilo;

import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class Uploader implements Runnable{

    private final LinkedBlockingQueue<Export> uploadQueue;
    private final LinkedBlockingQueue<Export> emailQueue;
    private final String apiKey;
    private final String apiSecret;
    private String projectID;
    static int threadsRunning = 0;
    private boolean running = true;

    public Uploader(LinkedBlockingQueue<Export> uploadQueue, LinkedBlockingQueue<Export> emailQueue, String apiKey, String apiSecret) {
        this.uploadQueue = uploadQueue;
        this.emailQueue = emailQueue;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        try {
            this.projectID = UploadThread.getProjectID(apiKey, apiSecret);
        } catch (IOException e) {
            Util.err("Kon projectID niet ophalen: " + e.getMessage());
            Util.err("Uploaden kan niet gestart worden");
            Main.exit();
            Util.exceptions.add(e);

        }

    }



    @Override
    public void run() {
        Util.success("Uploader gestart");
        while (running) {
            try {
                Export export = uploadQueue.take();
                if (export instanceof Exit) {
                    if (threadsRunning <= 0) {
                        running = false;
                        emailQueue.put(export);
                        break;
                    } else {
                        uploadQueue.put(export);
                        continue;
                    }

                }
                Thread uploadThread = new UploadThread(export, apiKey, apiSecret, projectID, emailQueue);
                uploadThread.start();
                threadsRunning++;
            } catch (InterruptedException e) {
                Util.err("Kon bestand niet uploaden: " + e.getMessage());
            }
        }
        Util.success("Uploader gestopt");
    }

    public static boolean noThreadsRunning() {
        return threadsRunning <= 0;
    }

}
