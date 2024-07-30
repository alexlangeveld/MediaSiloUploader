package nl.alexflix.mediasilouploader.remote.mediasilo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.MultiPartUploadTicket;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.UploadTicket;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiPartUploadThread extends UploadThread {


    public MultiPartUploadThread(Export export, String APIkey, String APIsecret, String projectID, LinkedBlockingQueue<Export> emailQueue) {
        super(export, APIkey, APIsecret, projectID, emailQueue);
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
            String filePath = export.OutputFile().getAbsolutePath();
        }
        String accessKey = uploadTicket.getAccessKey();
        String secretKey = uploadTicket.getSecretKey();
        String sessionId = uploadTicket.getSessionId();
        String sessionToken = uploadTicket.getSessionToken();
        String objectKey = uploadTicket.getObjectKey();
        String bucketName = uploadTicket.getBucketName();

        Util.log("\nsuper.uploadTicket: " + super.uploadTicket);

        String globalSessionId = sessionId;
        Regions region = Regions.US_EAST_1;

        File uploadFile = export.OutputFile();
        long totalBytes = uploadFile.length();

        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                accessKey, secretKey, sessionToken);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .withRegion(region)
                .build();

        TransferManager tx = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .build();

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, new FileInputStream(uploadFile), null);

        Upload upload = tx.upload(request);

        upload.addProgressListener(new ProgressListener() {
            private long bytesTransferred = 0;
            private int progressPercentage = 0;
            public void progressChanged(ProgressEvent progressEvent) {
                bytesTransferred += progressEvent.getBytesTransferred();
                int progressPercentage = (int) ((bytesTransferred * 100) / totalBytes);
                if (progressPercentage > this.progressPercentage) {
                    this.progressPercentage = progressPercentage;
                    export.setUploadProgress(progressPercentage);
                    Util.s3log("Upload progress: " + progressPercentage + "%");
                }
            }
        });


        try {
            UploadResult uploadResult = upload.waitForUploadResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Util.err(e);
        }

        tx.shutdownNow(false);
    }

}

