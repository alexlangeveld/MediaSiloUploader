package nl.alexflix.mediasilouploader.local.types;

import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.MediaInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class Export {




    public enum Status {
        QUEUED_TRANSCODE_LOCAL,
        TRANSCODING_LOCAL,
        QUEUED_UPLOAD,
        UPLOADING,
        TRANSCODING_REMOTE,
        QUEUED_EMAIL,
        EMAIL_SENT,
        QUEUED_DELETE,
        DELETED
    }

    private Status status;
    private File inputFile;
    private Map<String, String> metadata;
    private String[] naamElementen;
    protected String naam;
    private File outputFile;
    private String assetID;
    private int localTranscodeProgress = 0;
    private int uploadProgress = 0;
    private int remoteTranscodeProgress = 0;
    private boolean emailSent = false;
    private LocalDateTime timeOfDeath;
    private ArrayList<String> emails = new ArrayList<>();
    private boolean TC = true;
    private boolean HQ = false;
    private boolean downloadbaar = true;
    private String link;

    public Export(File inputFile) {
        this.inputFile = inputFile;
        this.metadata = MediaInfo.extractMetadata(inputFile.getPath());
        this.naamElementen = inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.')).split(";");
        this.naam = naamElementen[0];
        for (int i = 1; i < naamElementen.length; i++) {
            if (naamElementen[i].contains("@")) emails.add(naamElementen[i]);
            if (naamElementen[i].equalsIgnoreCase("geenTC")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("noTC")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("clean")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("downloadbaar")) downloadbaar = true;
            if (naamElementen[i].equalsIgnoreCase("nietDownloadbaar")) downloadbaar = false;
            if (naamElementen[i].equalsIgnoreCase("hq")) HQ = true;
        }
        Util.log("Nieuwe export aangemaakt: " + naam);
        this.status = Status.QUEUED_TRANSCODE_LOCAL;
    }

    protected Export() { }

    @Override
    public String toString() {
        return naam;
    }

    public String toSubString(int length) {
        String str = toString();
        int lengte = Math.min(length, str.length());
        return toString().substring(0, lengte);
    }

    public Status Status() {
        return status;
    }

    public void Status(Status status) {
        this.status = status;
    }

    public String[] getEmails() {
        return emails.toArray(new String[0]);
    }

    public String inputPath() {
        return inputFile.getPath();
    }
    public boolean TC() {
        return TC;
    }

    public double framerate() {
        try {
            return Double.parseDouble(metadata.get("FrameRate"));
        } catch (RuntimeException e) {
            e.printStackTrace();
            Util.err("GEBRUIK STANDAARD FRAMERATE: 25");
            return 25;
        }
    }

    public String startTimecode() {
        String start;
        try {
            start = metadata.get("TimeCode_FirstFrame");
        } catch (RuntimeException e) {
            start = "00:00:00:00";
            Util.err("GEEN TIJDCODE GEVONDEN: " + e.getMessage());
            Util.err("GEBRUIK DE STANDAARD START TC: " + start);
            return start;
        }
        return start;
    }

    public boolean HQ() {
        return HQ;
    }

    public File OutputFile() {
        if (outputFile == null) {
             String outputPath = inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf(File.separator)) + File.separator + naam + ".mp4";
            try {
                this.outputFile = new File(outputPath);
                if (this.outputFile.exists()) {
                    outputPath = inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf(File.separator)) + File.separator + naam + "_"   + (new Random().nextLong(1000000000L,9999999999L)) + ".mp4";
                    this.outputFile = new File(outputPath);
                }
            } catch (Exception e) {
                Util.err("FOUT: " + e.getMessage());
            }
        }
        return this.outputFile;
    }

    public void setTranscodeProgress(String line) {
        int old = this.localTranscodeProgress;
        String frameString = line.substring(line.indexOf("frame=") + 6, 11).trim();
        try {
            int progress = Integer.parseInt(frameString);
            int totalFrames = Integer.parseInt(metadata.get("FrameCount"));
            this.localTranscodeProgress = progress * 100 / totalFrames;
        } catch (NumberFormatException e) {
            // e.printStackTrace();
        }
            if (old != this.localTranscodeProgress) Util.log(naam + " transcoderen: " + this.localTranscodeProgress + "%");
    }
    public void setTranscodeProgress(int i) {
        this.localTranscodeProgress = i;
    }

    public void setAssetID(String id) {
        this.assetID = id;
    }

    public String getAssetID() {
        return assetID;
    }

    public boolean isDownloadbaar() {
        return downloadbaar;
    }

    public String getLink() {
        // als link null is return link die alleen bekeken kan worden door ingelogde gebruikers met toegang tot project
        if (link == null) return "https://app.shift.io/view/" + getAssetID();
        return link;
    }

    public void setLink(String linkID) {
        this.link = "https://app.shift.io/review/" + linkID;
    }

    public File getInputFile() {
        return inputFile;
    }

    public void moveFiles(String destinationDir) throws IOException {
            Path destinationPath = Paths.get(destinationDir);

            if (!Files.exists(destinationPath)) {
                Files.createDirectories(destinationPath);
            }

            Path sourceFilePath = this.inputFile.toPath();
            Path destinationFilePath = destinationPath.resolve(this.inputFile.getName());
            Files.move(sourceFilePath, destinationFilePath);

            if (this.outputFile != null && this.outputFile.exists()) {
                Path sourceOutputFilePath = this.outputFile.toPath();
                Path destinationOutputFilePath = destinationPath.resolve(this.outputFile.getName());
                Files.move(sourceOutputFilePath, destinationOutputFilePath);
            }

            this.outputFile = new File(destinationFilePath.toString());
            this.inputFile = new File(destinationFilePath.toString());
            Util.log("Bestanden verplaatst naar: " + destinationDir);


    }

    public int getLocalTranscodeProgress() {
        return localTranscodeProgress;
    }

    public int getUploadProgress() {
        return uploadProgress;
    }

    public void setUploadProgress(int uploadProgress) {
        this.uploadProgress = uploadProgress;
    }

    public int getRemoteTranscodeProgress() {
        return Math.abs(remoteTranscodeProgress);
    }

    public void setRemoteTranscodeProgress(int remoteTranscodeProgress) {
        this.remoteTranscodeProgress = remoteTranscodeProgress;
    }

    public LocalDateTime getTimeOfDeath() {
        return timeOfDeath;
    }

    public void setTimeOfDeath(LocalDateTime timeOfDeath) {
        this.timeOfDeath = timeOfDeath;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }
}
