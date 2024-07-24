package nl.alexflix.mediasilouploader.remote.email;


import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Util;
import org.json.JSONException;
import org.json.JSONObject;


public class Email {
    private Export export;
    private String emailBody;
    private String onderwerp;
    private String fromAdress;
    private String[] toAddresses;
    private String[] ccAddresses = {};;


    public Email(Export export, JSONObject emailTemplate, String van) {
        try {
            this.export = export;
            this.emailBody = emailTemplate.getString("emailBody").replace("$naam", this.export.toString()).replace("$link", export.getLink());
            this.onderwerp = emailTemplate.getString("onderwerp").replace("$naam", this.export.toString());
            this.fromAdress = van;
            this.toAddresses = new String[] {emailTemplate.getString("aan")};
            this.ccAddresses = export.getEmails();
        } catch (JSONException e) {
            Util.err("Kon email niet aanmaken: " + e.getMessage());
        }


    }


    public String getEmailBody() {
        return emailBody;
    }

    public String getOnderwerp() {
        return onderwerp;
    }

    public String getFromAdress() {
        return fromAdress;
    }

    public String[] getToAddresses() {
        return toAddresses;
    }

    public String[] getCcAddresses() {
        return ccAddresses;
    }
}
