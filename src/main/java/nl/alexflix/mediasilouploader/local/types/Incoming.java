package nl.alexflix.mediasilouploader.local.types;

import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.display.SimpleDisplay;
import nl.alexflix.mediasilouploader.display.SwingDisplay;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;

public class Incoming extends Export {
    File file;
    String[] naamElementen;
    String naam;
    int cyclesMissing = 0;
    boolean fileExists;
    private boolean hide = false;

    public Incoming(File file) {
        this.file = file;
        this.naamElementen = file.getName().split(";");
        this.naam = naamElementen[0];
    }

    public boolean isHidden() {
        return hide;
    }

    @Override
    public String toString() {
        fileExists = file.exists();
        if (cyclesMissing >= 10) hide = true;
        if (Main.display instanceof SwingDisplay) {
            String htmlYellow = "<font color='" + "#FFFF00" + "'>";
            String htmlEndFont = "</font>";
            String htmlRed = "<font color='" + "#FF0000" + "'>";

            if (fileExists) {
                return htmlYellow + "=>  " + naam + htmlEndFont;
            } else {
                cyclesMissing++;
                return htmlRed + "  X " + naam + htmlEndFont;
            }

        } else {
            if (fileExists) {
                return Util.ANSI_BOLD_HIGH_INTENSITY_YELLOW + "=>  " + naam + Util.ANSI_RESET;
            } else {
                cyclesMissing++;
                return Util.ANSI_RED + "  X " + naam + Util.ANSI_RESET;
            }
        }
    }

    @Override
    public String toSubString(int length) {
        int extraChars = toString().length() - naam.length() - 4;
        return super.toSubString(length + extraChars);
    }

    @Override
    public int getLocalTranscodeProgress() {
        return 0;
    }

    @Override
    public int getUploadProgress() {
        return 0;
    }

    @Override
    public int getRemoteTranscodeProgress() {
        return 0;
    }

    @Override
    public LocalDateTime getTimeOfDeath() {
        return null;
    }

    @Override
    public boolean isEmailSent() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incoming incoming = (Incoming) o;
        return Objects.equals(file.getAbsolutePath(), incoming.file.getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(file.getAbsolutePath());
    }
}
