package nl.alexflix.mediasilouploader.local.types;

import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.MediaInfo;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.Project;

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
    private boolean skipTranscode = false;
    private Project project;
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
    private int maxBitrate = 2500;
    private boolean downloadbaar = true;
    private boolean sendEmail = true;
    private String link;

    public Export(File inputFile) {
        this.inputFile = inputFile;
        this.metadata = MediaInfo.extractMetadata(inputFile.getPath());
        this.maxBitrate = berekenMaxumumBitrateOmNietOver5GBteGaan();
        this.naamElementen = inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.')).split(";");
        this.naam = naamElementen[0];
        for (int i = 1; i < naamElementen.length; i++) {
            naamElementen[i] = naamElementen[i].trim();
            if (naamElementen[i].contains("@")) emails.add(naamElementen[i]);
            if (naamElementen[i].equalsIgnoreCase("geenTC")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("noTC")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("clean")) TC = false;
            if (naamElementen[i].equalsIgnoreCase("downloadbaar")) downloadbaar = true;
            if (naamElementen[i].equalsIgnoreCase("nietDownloadbaar")) downloadbaar = false;
            if (naamElementen[i].equalsIgnoreCase("noDL")) downloadbaar = false;
            if (naamElementen[i].equalsIgnoreCase("hq")) HQ = true;
            if (naamElementen[i].equalsIgnoreCase("noEmail")) sendEmail = false;
            if (naamElementen[i].equalsIgnoreCase("skipTranscode")) skipTranscode = true;
            if (naamElementen[i].equalsIgnoreCase("noTranscode")) skipTranscode = true;
            if (inputFile.getName().substring(inputFile.getName().lastIndexOf('.')).equalsIgnoreCase(".MP4")) skipTranscode = true;
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
        String str = "  " + this;
        int strLength = str.length();
        int len = Math.min(length, strLength);
        String result = str.substring(0, len);

        // Pad the result if it's shorter than the specified length
        if (len < length) {
            StringBuilder paddedResult = new StringBuilder(result);
            while (paddedResult.length() < length) {
                paddedResult.append(' '); // Append spaces to the end
            }
            result = paddedResult.toString();
        }

        return result;
    }

    private int berekenMaxumumBitrateOmNietOver5GBteGaan() {
        double maxBitrate = 2500; // Default bitrate if calculations fail
        final long maxFileSizeBytes = 5L * 1024 * 1024 * 1024; // 5GB in bytes

        try {
            int totalFrames = Integer.parseInt(metadata.get("FrameCount"));
            double frameRate = Double.parseDouble(metadata.get("FrameRate"));
            double durationInSeconds = totalFrames / frameRate;

            // Convert 5GB to bits (since bitrate is in bits per second)
            long maxFileSizeBits = maxFileSizeBytes * 8;

            // Calculate the maximum allowable bitrate in bits per second
            maxBitrate = maxFileSizeBits / durationInSeconds / 1000; // Convert to kbps

        } catch (NumberFormatException e) {
            Util.err("Kon maximale bitrate niet berekenen: " + e.getMessage());
            Util.err(e);
        }

        return (int) Math.floor(maxBitrate);
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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
            int localTranscodeProgress = progress * 100 / totalFrames;
            if (localTranscodeProgress > this.localTranscodeProgress) {
                this.localTranscodeProgress = localTranscodeProgress;
            }
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

        Path sourceInputFilePath = this.inputFile.toPath();
        Path destinationInputFilePath = destinationPath.resolve(this.inputFile.getName());
        Files.move(sourceInputFilePath, destinationInputFilePath);

        this.inputFile = new File(destinationInputFilePath.toString());

        Path destinationOutputFilePath;
        if (this.outputFile != null && this.outputFile.exists()) {
            Path sourceOutputFilePath = this.outputFile.toPath();
            destinationOutputFilePath = destinationPath.resolve(this.outputFile.getName());
            Files.move(sourceOutputFilePath, destinationOutputFilePath);
        } else {
            destinationOutputFilePath = destinationPath.resolve(this.inputFile.getName()
                    .replace(".mxf", ".mp4"));
        }

        this.outputFile = new File(destinationOutputFilePath.toString());

        Util.log("Bestanden verplaatst naar: " + destinationDir);


    }

    public int getLocalTranscodeProgress() {
        return localTranscodeProgress;
    }

    public boolean checkIfFileBiggerThanXGB(int gigaBytes) {
        long GB = gigaBytes;
        if (this.OutputFile().length() > GB * 1024 * 1024 * 1024) {
            Util.log("Bestand " + this + " is groter dan 5GB");
            return true;
        }
        Util.log(this + " is kleiner dan 5GB");
        return false;
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

    public boolean sendEmail() {
        return sendEmail;
    }

    public boolean skipTranscode() {
        return skipTranscode;
    }

    public int getMaxBitrate() {
        return maxBitrate;
    }
}
