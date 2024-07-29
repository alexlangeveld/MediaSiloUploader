package nl.alexflix.mediasilouploader.remote.mediasilo.api;

import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class MultiPartUploadTicket extends UploadTicket {
    String fileName;
    long expiration;
    String accessKey;
    String secretKey;
    String sessionId;
    String sessioToken;
    String objectKey;
    String bucketName;

    public MultiPartUploadTicket(Response response) throws IOException {
        super(response);
        JSONObject ticketJson = new JSONObject(response.body().string());
        this.fileName = ticketJson.getString("fileName");
        super.assetUrl = ticketJson.getString("assetUrl");
        this.expiration = ticketJson.getLong("expiration");
        this.accessKey = ticketJson.getString("accessKey");
        this.secretKey = ticketJson.getString("secretKey");
        this.sessionId = ticketJson.getString("sessionId");
        this.sessioToken = ticketJson.getString("sessionToken");
        this.objectKey = ticketJson.getString("objectKey");
        this.bucketName = ticketJson.getString("bucketName");
    }

    public String getFileName() {
        return fileName;
    }

    public long getExpiration() {
        return expiration;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessioToken() {
        return sessioToken;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }
}
