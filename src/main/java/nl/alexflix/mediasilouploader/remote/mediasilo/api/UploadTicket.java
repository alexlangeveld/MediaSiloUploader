package nl.alexflix.mediasilouploader.remote.mediasilo.api;

import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class UploadTicket {
    private final String assetUrl;
    private final String authorization;
    private final String amzAcl;
    private final String contentType;
    private final String amzDate;
    private final String httpMethod;

    public UploadTicket(Response response) throws IOException {
        JSONObject ticketJson = new JSONObject(response.body().string());
        this.assetUrl = ticketJson.getString("assetUrl");
        this.authorization = ticketJson.getString("authorization");
        this.amzAcl = ticketJson.getString("amzAcl");
        this.contentType = ticketJson.getString("contentType");
        this.amzDate = ticketJson.getString("amzDate");
        this.httpMethod = ticketJson.getString("httpMethod");
    }

    public String getAssetUrl() {
        return assetUrl;
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getAmzAcl() {
        return amzAcl;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAmzDate() {
        return amzDate;
    }

    public String getHttpMethod() {
        return httpMethod;
    }
}
