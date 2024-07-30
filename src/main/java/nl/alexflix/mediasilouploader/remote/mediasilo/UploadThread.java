package nl.alexflix.mediasilouploader.remote.mediasilo;

import com.google.gson.Gson;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.Project;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.UploadTicket;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


public class UploadThread extends Thread{
    final Export export;
    String APIkey;
    String APIsecret;
    String projectID;
    OkHttpClient client;
    UploadTicket uploadTicket;
    final LinkedBlockingQueue<Export> emailQueue;

    public UploadThread(Export export, String APIkey, String APIsecret, String projectID, LinkedBlockingQueue<Export> emailQueue) {
        this.projectID = projectID;
        this.emailQueue = emailQueue;
        super.setName(("UploadThread-" + export.toString()));
        this.export = export;
        this.APIkey = APIkey;
        this.APIsecret = APIsecret;
        this.client = new OkHttpClient();
    }



    @Override
    public void run() {
        boolean succes = upload(export);
        if (succes) {
            try {
                Util.success("Upload van + " + export + " succesvol");
                emailQueue.put(export);
                Util.log(export + " toevoegen aan e-mail wachtrij");
            } catch (InterruptedException e) {
                Util.err("Kon bestand niet toevoegen aan e-mail wachtrij: " + e.getMessage());
            }
        } else {
            Util.err("Upload van " + export + " mislukt");
        }
        Uploader.threadsRunning--;
    }

    boolean upload(Export export) {
        try {
            if (export.checkIfFileBiggerThanXGB(5)) {
                throw new RuntimeException("Bestand is groter dan 5GB");
            }
            createUploadTicket();
            uploadFile();
            createAsset();
            checkEncodingProgress();
            createReviewLink();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Util.err("Upload mislukt: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                Util.err(element.toString());
            }
            return false;
        }
        return true;
    }



    void createUploadTicket() throws IOException {
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody ticketRequestBody = RequestBody.create(mediaType, "{ \"fileName\": \"" + export.OutputFile().getName() + "\" }");



        Request ticketRequest = new Request.Builder()
                .url("https://api.shift.io/v3/assets/upload")
                .post(ticketRequestBody)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("x-key", APIkey)
                .addHeader("x-secret", APIsecret)
                .build();

        Util.http(ticketRequest.toString());


        Response ticketResponse = client.newCall(ticketRequest).execute();
        Util.http(ticketResponse.toString());

        if (!ticketResponse.isSuccessful()) {
            throw new IOException("Kon geen uploadTicket voor " + export + " aanmaken: " + ticketResponse);
        }
        Util.success("UploadTicket voor " + export + " succesvol aangemaakt");
        this.uploadTicket = new UploadTicket(ticketResponse);

    }

    protected void uploadFile() throws IOException {
        // Create request body
        RequestBody requestBody = RequestBody.create(export.OutputFile(), MediaType.parse(uploadTicket.getContentType()));

        // Build the request
        Request uploadRequest = new Request.Builder()
                .url(uploadTicket.getAssetUrl())
                .put(requestBody)  // This assumes the httpMethod is always PUT
                .addHeader("Authorization", uploadTicket.getAuthorization())
                .addHeader("x-amz-acl", uploadTicket.getAmzAcl())
                .addHeader("Content-Type", uploadTicket.getContentType())
                .addHeader("x-amz-date", uploadTicket.getAmzDate())
                .build();

        // Execute the request
        Util.http(uploadRequest.body().toString());
        try (Response uploadResponse = client.newCall(uploadRequest).execute()) {
            Util.http(uploadResponse.body().string());
            if (!uploadResponse.isSuccessful()) {
                throw new IOException("Kon bestand" + export + "niet uploaden: " + uploadResponse);
            }
            export.setUploadProgress(100);
            Util.success("Bestand " + export + " succesvol geupload");
        }
    }
    void createAsset() throws IOException {
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

        Util.http(assetCreateRequest.body().toString());
        try (Response assetCreateResponse = client.newCall(assetCreateRequest).execute()) {
            String response = assetCreateResponse.body().string();
            Util.http(response);
            JSONObject jsonObject = new JSONObject(response);
            export.setAssetID(jsonObject.getString("id"));
            if (!assetCreateResponse.isSuccessful()) {
                throw new IOException("Kon asset niet aanmaken: " + assetCreateResponse);
            } else Util.success("Asset succesvol aangemaakt");

        } catch (JSONException e) {
            Util.err("Kon JSON response niet parsen: " + e.getMessage());
            throw e;
        }

    }

    void checkEncodingProgress() {
        boolean completed = false;
        while (!completed) {

            Request request = new Request.Builder()
                    .url("https://api.shift.io/v3/assets/" + export.getAssetID() + "/encode/progress")
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("x-secret", this.APIsecret)
                    .addHeader("x-key", this.APIkey)
                    .build();
            Util.http(request.toString());

            try (Response response = client.newCall(request).execute()){
                String responseBody = response.body().string();
                Util.http(responseBody);
                if (!response.isSuccessful()) {
                    throw new IOException(response.body().string());
                }
                JSONObject jsonObject = new JSONObject(responseBody);
                int percent = jsonObject.getInt("progress");
                export.setRemoteTranscodeProgress(percent);
                Util.log(export + " omzetten op MediaSilo: " + percent + "%");
                if (percent >= 100) {
                    Util.success(export + " succesvol omgezet op MediaSilo");
                    export.setRemoteTranscodeProgress(100);
                    completed = true;
                }
            } catch (IOException e) {
                Util.err("Oeps. Kan encoding niet controleren." + e.getMessage());
            }

            // slapen voor een halve minuut
            try {
                if (!completed) Thread.sleep(30000);
            } catch (InterruptedException e) {
                Util.err("Thread had een nachtmerrie en kon niet slapen: " + e.getMessage());
            }
        }
    }


    void createReviewLink() throws IOException {
        // Construct JSON object for review link data
        JSONObject reviewLinkData = new JSONObject();

        // Asset IDs (required)
        JSONArray assetIds = new JSONArray();
        // Add asset IDs as needed
        assetIds.put(export.getAssetID());
        reviewLinkData.put("assetIds", assetIds);

        // Folder IDs (optional, empty in this case)
        JSONArray folderIds = new JSONArray();
        reviewLinkData.put("folderIds", folderIds);

        // Authorized User IDs (optional, empty in this case)
        JSONArray authorizedUserIds = new JSONArray();
        reviewLinkData.put("authorizedUserIds", authorizedUserIds);

        // Configuration object (required)
        JSONObject configuration = new JSONObject();
        configuration.put("allowDownload", export.isDownloadbaar());
        configuration.put("allowFeedback", false);
        configuration.put("audience", "public");
        configuration.put("emailNotification", false);
        configuration.put("limitCommentsToLink", false);
        configuration.put("password", "");
        configuration.put("watermarked", false);
        reviewLinkData.put("configuration", configuration);

        // Description (optional)
        reviewLinkData.put("description", ("Export van " + export + " op " + Util.datum() + " om " + Util.tijd()));

        // Expiration date (required)
        reviewLinkData.put("expires", 0);  // 0 means link does not expire

        // Watermark preset ID (optional, null in this case)
        reviewLinkData.put("watermarkPresetId", JSONObject.NULL);

        // Watermark is forensic (optional, false in this case)
        reviewLinkData.put("watermarkIsForensic", false);

        // Watermark settings (optional, null in this case)
        reviewLinkData.put("watermarkSettings", JSONObject.NULL);

        // Title (required)
        reviewLinkData.put("title", export.toString());

        // Create request body
        RequestBody reviewLinkRequestBody = RequestBody.create(MediaType.parse("application/json"), reviewLinkData.toString());

        // Build the request
        Request reviewLinkCreateRequest = new Request.Builder()
                .url("https://api.shift.io/v4/sync/links")
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("x-key", this.APIkey)
                .addHeader("x-secret", this.APIsecret)
                .post(reviewLinkRequestBody)
                .build();

        // Execute the request
        try (Response reviewLinkCreateResponse = client.newCall(reviewLinkCreateRequest).execute()) {
            if (!reviewLinkCreateResponse.isSuccessful()) {
                throw new IOException("Kon geen linkje maken: " + reviewLinkCreateResponse);
            }

            // Optionally, you can handle the response body here if needed
            String responseBody = reviewLinkCreateResponse.body().string();
            Util.http(responseBody);
            JSONObject jsonObject = new JSONObject(responseBody);
            String linkID = jsonObject.getString("id");
            export.setLink(linkID);
            Util.success("Linkje is gemaakt: " + export.getLink());


        }
    }











    public static String getProjectID(String apiKey, String apiSecret) throws IOException {
        List<Project> projects = getAllProjects(apiKey, apiSecret);
        for (Project project : projects) {
            if (project.getName().equals(Main.ProjectNaam())) {
                return project.getId();
            }
        }
        return "41de6ee4-a6f0-442f-b2ae-1e0cee74e7e8";
    }
    public static ArrayList<Project> getAllProjects(String APIkey, String APIsecret) throws IOException {
        final String API_URL = "https://api.shift.io/v3/projects";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("x-key", APIkey)
                .addHeader("x-secret", APIsecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Gson gson = new Gson();
            Type projectListType = new TypeToken<ArrayList<Project>>(){}.getType();
            return gson.fromJson(responseBody, projectListType);
        }
    }

    public static void main(String[] args) {
        try {
            List<Project> projects = getAllProjects("17e052e0-6eb7-4e8d-b3a0-08df142485c6", "6a0b351acdeb3b1ca95fdb7d5d8e57f7");
            for (Project project : projects) {
                System.out.println(project);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
