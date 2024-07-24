package nl.alexflix.mediasilouploader.remote.mediasilo;

import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.UploadTicket;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiPartUploadThread extends UploadThread {

    UploadTicket uploadTicket;

    public MultiPartUploadThread(Export export, String APIkey, String APIsecret, String projectID, LinkedBlockingQueue<Export> emailQueue) {
        super(export, APIkey, APIsecret, projectID, emailQueue);
    }

    private void MultiPartUploadFile() {

    }

    @Override
    boolean upload(Export export) {
        try {
            createUploadTicket();
            uploadFile();
            createAsset();
            checkEncodingProgress();
            createReviewLink();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Util.err("Upload mislukt: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    void createUploadTicket() throws IOException {
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody ticketRequestBody = RequestBody.create(mediaType, "{ \"fileName\": \"" + export.OutputFile().getName() + "\" }");



        Request ticketRequest = new Request.Builder()
                .url("https://api.shift.io/v3/assets/multipart/upload")
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

        this.uploadTicket = new UploadTicket(ticketResponse);
    }

    @Override
    protected void uploadFile() throws IOException {
        super.uploadFile();
    }
}
