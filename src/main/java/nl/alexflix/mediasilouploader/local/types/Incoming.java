package nl.alexflix.mediasilouploader.local.types;

import nl.alexflix.mediasilouploader.Util;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;

public class Incoming extends Export {
    String naam;

    public Incoming(File file) {
        String[] naamelementen = file.getName().split(";");
        this.naam = naamelementen[0];
    }

    @Override
    public String toString() {
        return this.naam;
    }

    @Override
    public String toSubString(int length) {
        String str = this.toString();
        int lengte = Math.min(length, str.length());
        return Util.ANSI_YELLOW + toString().substring(0, lengte) + Util.ANSI_RESET;
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
        return Objects.equals(naam, incoming.naam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(naam);
    }
}
