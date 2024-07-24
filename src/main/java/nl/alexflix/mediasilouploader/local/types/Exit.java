package nl.alexflix.mediasilouploader.local.types;

public class Exit extends Export{
    public Exit() {
        super();
        super.naam = "Exit";
        super.setTranscodeProgress(0);
        super.setUploadProgress(0);
        super.setRemoteTranscodeProgress(0);
        super.setEmailSent(false);
//        super.setTimeOfDeath(LocalDateTime.now());
    }

//    @Override
//    public LocalDateTime getTimeOfDeath() {
//        return LocalDateTime.now();
//    }
    @Override
    public String toString() {
        return "\u001B[31m" + "   -- EXIT --       " + "\u001B[0m";
    }
}
