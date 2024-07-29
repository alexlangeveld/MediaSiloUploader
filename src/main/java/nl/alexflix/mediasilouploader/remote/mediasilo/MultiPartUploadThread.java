package nl.alexflix.mediasilouploader.remote.mediasilo;

import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.MultiPartUploadTicket;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.UploadTicket;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiPartUploadThread extends UploadThread {


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

        super.uploadTicket = new MultiPartUploadTicket(ticketResponse);
    }

    @Override
    protected void uploadFile() throws IOException {
        MultiPartUploadTicket uploadTicket;
        if (!(super.uploadTicket instanceof MultiPartUploadTicket)) {
            throw new IOException("uploadTicket is geen MultiPartUploadTicket");
        } else {
            uploadTicket = (MultiPartUploadTicket) super.uploadTicket;
            String filePath = multiPartUploadTicket.getFilePath();
        }
        String fileName = uploadTicket.getFileName();
        String assetUrl = uploadTicket.getAssetUrl();
        String expiration = Long.toString(uploadTicket.getExpiration());
        String accessKey = uploadTicket.getAccessKey();
        String secretKey = uploadTicket.getSecretKey();
        String sessionId = uploadTicket.getSessionId();
        String sessionToken = uploadTicket.getSessionToken();
        String objectKey = uploadTicket.getObjectKey();
        String bucketName = uploadTicket.getBucketName();

        System.out.println("\nsuper.uploadTicket: " + super.uploadTicket);

        globalSessionId = sessionId;

        File uploadFile = new File(filePath);

        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                accessKey, secretKey, sessionToken);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .build();

        TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .build();

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, new FileInputStream(uploadFile), null);

        Upload upload = tm.upload(request);

        upload.addProgressListener(new ProgressListener() {
            public void progressChanged(ProgressEvent progressEvent) {
                System.out.println(progressEvent);
            }
        });

        try {
            UploadResult uploadResult = upload.waitForUploadResult();
            createAsset(assetUrl);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tm.shutdownNow(false);
    }

}
}
