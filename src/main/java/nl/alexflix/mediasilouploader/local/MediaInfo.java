package nl.alexflix.mediasilouploader.local;

import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaInfo {
    public static final String MEDIAINFO_PATH = Main.mediainfoPath();
    public static Map<String, String> extractMetadata(String filePath) {
        Map<String, String> metadata = new HashMap<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(MEDIAINFO_PATH, "--Output=XML", filePath);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder xmlOutput = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                xmlOutput.append(line);
                //Util.log(line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Extract key-value pairs from XML using regex
                Pattern pattern = Pattern.compile("<(.+?)>(.+?)</(.+?)>");
                Matcher matcher = pattern.matcher(xmlOutput.toString());

                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    Util.log("[Minf] " + key + ": " + value);
                    metadata.put(key, value);
                }
            } else {
                Util.err("Er ging iets mis met Mediainfo Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            Util.err("Er ging iets mis met Mediainfo: " + e.getMessage());
        }

        return metadata;
    }

    public static boolean isClosed(String filePath) {
        boolean closed = false;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(MEDIAINFO_PATH, "--Output=XML", filePath);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            String filename = filePath.substring(filePath.lastIndexOf("\\") + 1);
            while ((line = reader.readLine()) != null) {
                if (line.contains("<Format_Settings>Open / Incomplete</Format_Settings>")) {
                    Util.log(filename + " open, not yet complete");
                    closed = false;
                }
                else if (line.contains("<Format_Settings>Closed / Complete</Format_Settings>")) {
                    Util.success(filename + " compleet, good to go!");
                    closed = true;
                }

            }
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            if (finished) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    return closed;
                } else {
                    Util.err("Ging niet helemaal goed met MediaInfo. Exit code: " + exitCode);
                }
            } else Util.err("MediaInfo timeout");
        } catch (IOException | InterruptedException e) {
            Util.err("Er ging iets mis met MediaInfo: " + e.getMessage());
        }

       return closed;
    }
}

