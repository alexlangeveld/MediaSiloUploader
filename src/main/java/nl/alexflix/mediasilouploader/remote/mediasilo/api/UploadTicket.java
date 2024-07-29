package nl.alexflix.mediasilouploader.remote.mediasilo.api;

import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class UploadTicket {
    String assetUrl;
    String authorization;
    String amzAcl;
    String contentType;
    String amzDate;
    String httpMethod;

    public UploadTicket(Response response) throws IOException {
        try {
            JSONObject ticketJson = new JSONObject(response.body().string());
            this.assetUrl = ticketJson.getString("assetUrl");
            this.authorization = ticketJson.getString("authorization");
            this.amzAcl = ticketJson.getString("amzAcl");
            this.contentType = ticketJson.getString("contentType");
            this.amzDate = ticketJson.getString("amzDate");
            this.httpMethod = ticketJson.getString("httpMethod");
        } catch (JSONException e) {
            if (this instanceof MultiPartUploadTicket) {
                this.assetUrl = null;
                this.authorization = null;
                this.amzAcl = null;
                this.contentType = null;
                this.amzDate = null;
                this.httpMethod = null;
            } else {
                throw e;
            }
        }
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
