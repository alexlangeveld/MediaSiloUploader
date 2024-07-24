package nl.alexflix.mediasilouploader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger implements Runnable{
    private volatile boolean running = true;
    private final LinkedBlockingQueue<String> logs = new LinkedBlockingQueue<>();
    private final File file;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(String path) {
        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            running = false;
            Util.err("Logger UnknownHostException: " + e.getMessage());
            computerName = String.valueOf(new Random().nextLong());
        }
        String logFileName = File.separator + "MediaSiloUploader_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_" + computerName + ".log";
        this.file = new File(path + logFileName);

        logs.addAll(Util.allLogs);
    }

    public void add(String message) {
        logs.add(LocalDateTime.now().format(formatter) + " : " + message);
    }

    @Override
    public void run() {
        if (!running) return;
        Util.success("Logger gestart");
        try (FileWriter writer = new FileWriter(file, true)) {
            while (running || !logs.isEmpty()) {
                try {
                    String log = logs.take();
                    writer.write(log + "\n");
                } catch (InterruptedException e) {
                    Util.err("Logger InterruptedException: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    if (!running) break;
                }
            }
            writer.flush();
        } catch (IOException e) {
            Util.err("Logger IOException: " + e.getMessage());
            running = false;
            Thread.currentThread().interrupt();
        }
        Util.success("Logger gestopt");
    }

    public void stop() {
        running = false;
        Util.log("Logger stoppen");
    }
}
