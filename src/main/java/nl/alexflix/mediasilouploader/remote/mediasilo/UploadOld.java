package nl.alexflix.mediasilouploader.remote.mediasilo;

import nl.alexflix.mediasilouploader.remote.mediasilo.api.UploadTicket;
import okhttp3.*;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class UploadOld implements Runnable{
    private File file;
    private String APIkey;
    private String APIsecret;
    private String projectID;
    private OkHttpClient client;
    private UploadTicket uploadTicket;
    public UploadOld(File file, String APIkey, String APIsecret, String projectID) {
        this.file = file;
        this.APIkey = APIkey;
        this.APIsecret = APIsecret;
        this.projectID = projectID;
        this.client = new OkHttpClient();
    }

    private void createUploadTicket() throws IOException {
        RequestBody ticketRequestBody = new FormBody.Builder()
                .add("fileName", file.getName())
                .build();

        Request ticketRequest = new Request.Builder()
                .url("https://api.shift.io/v3/assets/upload")
                .addHeader("x-key", this.APIkey)
                .addHeader("x-secret", this.APIsecret)
                .post(ticketRequestBody)
                .build();

        Response ticketResponse = client.newCall(ticketRequest).execute();
        if (!ticketResponse.isSuccessful()) {
            throw new IOException("Failed to create upload ticket: " + ticketResponse);
        }

        this.uploadTicket = new UploadTicket(ticketResponse);
    }

    private void uploadFile() throws IOException {
        RequestBody fileBody = RequestBody.create(MediaType.parse(uploadTicket.getContentType()), file);
        Request s3UploadRequest = new Request.Builder()
                .url(uploadTicket.getAssetUrl())
                .put(fileBody)
                .addHeader("Authorization", uploadTicket.getAuthorization())
                .addHeader("x-amz-acl", uploadTicket.getAmzAcl())
                .addHeader("Content-Type", uploadTicket.getContentType())
                .addHeader("x-amz-date", uploadTicket.getAmzDate())
                .build();

        try (Response s3UploadResponse = client.newCall(s3UploadRequest).execute()) {
            if (!s3UploadResponse.isSuccessful()) {
                throw new IOException("Failed to upload file to S3: " + s3UploadResponse);
            }
        }
    }
    private void createAsset() throws IOException {
        JSONObject assetData = new JSONObject()
                .put("sourceUrl", uploadTicket.getAssetUrl())
                .put("projectId", this.projectID);

        RequestBody assetRequestBody = RequestBody.create(MediaType.parse("application/json"), assetData.toString());

        Request assetCreateRequest = new Request.Builder()
                .url("https://api.shift.io/v3/assets")
                .addHeader("x-key", this.APIkey)
                .addHeader("x-secret", this.APIsecret)
                .post(assetRequestBody)
                .build();

        Response assetCreateResponse = client.newCall(assetCreateRequest).execute();
        if (!assetCreateResponse.isSuccessful()) {
            throw new IOException("Failed to create asset: " + assetCreateResponse);
        }
    }

    @Override
    public void run() {
        try {
            createUploadTicket();
            uploadFile();
            createAsset();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
