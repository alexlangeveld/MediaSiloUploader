package nl.alexflix.mediasilouploader.remote.mediasilo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.remote.mediasilo.api.MultiPartUploadTicket;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.amazonaws.auth.BasicSessionCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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

//    @Override
//    protected void uploadFile() throws IOException {
//        MultiPartUploadTicket uploadTicket;
//        if (super.uploadTicket instanceof MultiPartUploadTicket) {
//            uploadTicket = (MultiPartUploadTicket) super.uploadTicket;
//        } else {
//            throw new IOException("uploadTicket is not a MultiPartUploadTicket");
//        }
//
//        String accessKey = uploadTicket.getAccessKey();
//        String secretKey = uploadTicket.getSecretKey();
//        String sessionToken = uploadTicket.getSessionToken();
//        String objectKey = uploadTicket.getObjectKey();
//        String bucketName = uploadTicket.getBucketName();
//        final Region region = Region.US_EAST_1;
//
//        // Use BasicSessionCredentials if sessionToken is provided
//        BasicSessionCredentials awsCredentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
//        AwsCredentials sessionCredentials = awsCredentials;
//
//        // Create the S3 client with StaticCredentialsProvider
//        S3Client s3 = S3Client.builder()
//                .region(region)
//                .credentialsProvider(new AWSStaticCredentialsProvider(awsCredentials))
//                .build();
//
//        // Initiate a multipart upload
//        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
//                .bucket(bucketName)
//                .key(objectKey)
//                .build();
//
//        CreateMultipartUploadResponse createResponse = s3.createMultipartUpload(createRequest);
//        String uploadId = createResponse.uploadId();
//
//        // Prepare the parts to be uploaded
//        List<CompletedPart> completedParts = new ArrayList<>();
//        int partNumber = 1;
//        ByteBuffer buffer = ByteBuffer.allocate(5 * 1024 * 1024); // 5 MB part size
//
//        try (RandomAccessFile file = new RandomAccessFile(export.OutputFile(), "r")) {
//            long fileSize = file.length();
//            long position = 0;
//
//            while (position < fileSize) {
//                file.seek(position);
//                int bytesRead = file.getChannel().read(buffer);
//
//                if (bytesRead == -1) {
//                    break;
//                }
//
//                buffer.flip();
//                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
//                        .bucket(bucketName)
//                        .key(objectKey)
//                        .uploadId(uploadId)
//                        .partNumber(partNumber)
//                        .contentLength((long) bytesRead)
//                        .build();
//
//                UploadPartResponse response = s3.uploadPart(uploadPartRequest,
//                        software.amazon.awssdk.core.sync.RequestBody.fromByteBuffer(buffer));
//
//                completedParts.add(CompletedPart.builder()
//                        .partNumber(partNumber)
//                        .eTag(response.eTag())
//                        .build());
//
//                buffer.clear();
//                position += bytesRead;
//                partNumber++;
//            }
//        } catch (IOException e) {
//            Util.err("MultiPartUploadThread IOException: " + e.getMessage());
//            Util.err(e);
//            // Abort the multipart upload on error
//            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
//                    .bucket(bucketName)
//                    .key(objectKey)
//                    .uploadId(uploadId)
//                    .build());
//            throw e;
//        }
//
//        // Complete the multipart upload
//        CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
//                .parts(completedParts)
//                .build();
//
//        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
//                .bucket(bucketName)
//                .key(objectKey)
//                .uploadId(uploadId)
//                .multipartUpload(completedUpload)
//                .build();
//
//        CompleteMultipartUploadResponse completeResponse = s3.completeMultipartUpload(completeRequest);
//
//        // Print the object's URL
//        String objectUrl = s3.utilities().getUrl(GetUrlRequest.builder()
//                        .bucket(bucketName)
//                        .key(objectKey)
//                        .build())
//                .toExternalForm();
//
//        Util.success("Uploaded object URL: " + objectUrl);
//    }



}

