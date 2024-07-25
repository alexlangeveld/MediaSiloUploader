package nl.alexflix.mediasilouploader.local;

import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Transcoder implements Runnable {

    private boolean running = true;
    private String ffmpegPath;
    private boolean overwrite = true;
    private String videoCodec;
    private LinkedBlockingQueue<Export> transcodeQueue;
    private LinkedBlockingQueue<Export> uploadQueue;

    public Transcoder(LinkedBlockingQueue<Export> transcodeQueue, LinkedBlockingQueue<Export> uploadQueue, String ffmpegPath) {
        this.transcodeQueue = transcodeQueue;
        this.uploadQueue = uploadQueue;
        this.ffmpegPath = ffmpegPath;
        if (checkEncoder("hevc_nvenc")) {
            Util.log("HEVC_NVENC gevonden, hardware versnelling ingeschakeld...");
            this.videoCodec = "hevc_nvenc";
        } else {
            Util.err("HEVC_NVENC codec niet gevonden. Gebruik codec 'libx265'");
            this.videoCodec = "libx265";
        }

    }

    @Override
    public void run() {
        Util.success("Transcoder gestart");
        while (running) {
            Export current = null;
            try {
                current = transcodeQueue.take();
                if (current instanceof Exit) {
                    running = false;
                    uploadQueue.put(current);
                    break;
                }
            } catch (InterruptedException e) {
                Util.err("Kon bestand niet ophalen uit wachtrij: " + e.getMessage());
                continue;
            }

            String[] command = getCommand(current);

            ProcessBuilder processBuilder = new ProcessBuilder(command);


            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();

                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        current.setTranscodeProgress(line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    Util.err("Kon niet transcoderen. Exit code: " + exitCode);
                } else {
                    Util.success("Transcoderen succesvol");
                    current.setTranscodeProgress(100);
                    uploadQueue.put(current);
                }
            } catch (IOException | InterruptedException e) {
                Util.err("Kon niet transcoderen: " + e.getMessage());
            }


        }
        Util.success("Transcoder gestopt");

    }

    private String[] getCommand(Export export) {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add(overwrite ? "-y" : "-n");
        cmd.add("-probesize");
        cmd.add("100M");
        cmd.add("-i");
        cmd.add(export.inputPath());
        if (export.TC()) {
            cmd.add("-vf");

            // ${ffmpegPath} -y -probesize 100M -i "${filePath}" -vf "yadif=0:-1:0, format=yuv420p, drawtext=fontfile=C\\:/Windows/fonts/Arial.ttf:timecode='${startTimecode}':rate=25:fontsize=48:fontcolor=white:box=1:boxcolor=black@0.5:x=48:y=48:boxborderw=8" -c:v hevc_nvenc -b:v 2.5M -preset fast -c:a aac -b:a 128k -colorspace bt709 -color_trc bt709 -color_primaries bt709 -color_range tv -movflags +faststart "$outputPath"
            StringBuilder vf = new StringBuilder();
            vf.append("\"");
            vf.append("yadif=0:-1:0, "); // niet deinterlacen
            vf.append("format=yuv420p, "); // maar wel altijd progessive exporteren
            vf.append("drawtext="); // begin tijdcode burn filter
            vf.append("'fontfile=C\\:/Windows/Fonts/Arial.ttf':");  //waar vinden we het lettertpe?
            vf.append("timecode="); // wat is de tijdcode?
            vf.append("\\'" + export.startTimecode() + "\\':"); // dit is de tijdcode :)
            vf.append("rate=" + export.framerate() + ":");  // hoe snel moet de tijdcode oplopen?
            vf.append("fontsize=48:"); // hoe groot moet de TC?
            vf.append("fontcolor=white:"); // witte letters pls
            vf.append("box=1:"); // box maken?
            vf.append("boxcolor=black@0.5:"); // kleur van de box
            vf.append("x=48:"); // x positie van de box
            vf.append("y=48:"); // y positie van de box
            vf.append("boxborderw=8"); // marge van de box
            vf.append("\"");


            cmd.add(vf.toString());
        } else {
            cmd.add("-vf");

            StringBuilder vf = new StringBuilder();
            vf.append("\"");
            vf.append("yadif=0:-1:0, "); // niet deinterlacen
            vf.append("format=yuv420p"); // maar wel altijd progessive exporteren
            vf.append("\"");

            cmd.add(vf.toString());
        }
        cmd.add("-c:v");
        cmd.add(videoCodec);

        cmd.add("-preset");
        cmd.add(export.HQ() ? "medium" : "fast");

        if (export.HQ()) {
            cmd.add("-rc");
            cmd.add("vbr");
            cmd.add("-cq");
            cmd.add("20");
            cmd.add("-qmin");
            cmd.add("20");
            cmd.add("-qmax");
            cmd.add("20");
        } else {
            cmd.add("-b:v");
            cmd.add("2.5M");
        }

        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(export.HQ() ? "192k" : "256k");
        cmd.add("-colorspace");
        cmd.add("bt709");
        cmd.add("-color_trc");
        cmd.add("bt709");
        cmd.add("-color_primaries");
        cmd.add("bt709");
        cmd.add("-color_range");
        cmd.add("tv");
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add(export.OutputFile().getPath());

//        System.out.println("FFmpeg command: ");
//        for (String s : cmd) {
//            System.out.print(s);
//            System.out.print(" ");
//        }
//        System.out.println();
        return cmd.toArray(new String[]{});
    }

    private boolean checkEncoder(String encoder) {
        try {
            // Execute the ffmpeg command to list encoders
            String command = ffmpegPath + " -encoders";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            boolean encoderListed = false;
            while ((line = reader.readLine()) != null) {
                // Check if the line contains 'hevc_nvenc'
                if (line.contains(encoder)) {
                    encoderListed = true;
                    break;
                }
            }

            if (!encoderListed) {
                return false;
            }

            // Attempt to encode a test video to verify actual hardware support
            String[] testCommand = {
                    ffmpegPath,
                    "-f", "lavfi",
                    "-i", "nullsrc=s=512x512:d=1",
                    "-c:v", encoder,
                    "-f", "null",
                    "-"
            };

            Process testProcess = Runtime.getRuntime().exec(testCommand);
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(testProcess.getErrorStream()));

            boolean hardwareSupport = true;
            while ((line = errorReader.readLine()) != null) {
                if (line.contains("No NVENC capable devices found")) {
                    hardwareSupport = false;
                    break;
                }
            }

            int exitValue = testProcess.waitFor();
            if (exitValue != 0 && hardwareSupport) {
                Util.err("ffmpeg command exited with error code: " + exitValue);
            }

            return hardwareSupport;
        } catch (IOException | InterruptedException e) {
            Util.err("Error checking encoder: " + e.getMessage());
        }
        return false;
    }

}
