package com.oussema.cloud.adapter;


import com.oussema.cloud.config.AwsS3BucketProperties;
import com.oussema.cloud.exception.StorageException;
import com.oussema.cloud.model.Blob;
import io.awspring.cloud.s3.S3Template;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class AwsStorageAdapter implements Storage {

    private static final String IMAGES_FOLDER = "images";
    private static final String FILES_FOLDER = "files";


    private final S3Template s3Template;
    private final S3Presigner s3Presigner;
    private final AwsS3BucketProperties awsS3BucketProperties;

    @Value("${cloud.storage.access-url-duration-minutes:30}")
    private long accessUrlDurationInMinutes;


    @PostConstruct
    public void logActiveAdapter() {
        log.info("AWS Adapter is active");
    }

    /**
     * Determines the appropriate subfolder based on file content type
     *
     * @param contentType the MIME type of the file
     * @return the name of the subfolder where the file should be stored
     */
    private String determineSubfolderForContentType(String contentType) {

        if (contentType.startsWith("image/")) {
            return IMAGES_FOLDER;
        } else {
            return FILES_FOLDER;
        }
    }

    /**
     * Uploads a file to the cloud storage
     *
     * @param file the file to upload
     * @return the URL of the uploaded file
     */
    @Override
    public Blob uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload attempt with null or empty file.");
            return null;
        }

        var originalFilename = file.getOriginalFilename();
        var bucketName = awsS3BucketProperties.s3().bucketName();

        // Determine subfolder based on file type
        String subfolder =
                determineSubfolderForContentType(Objects.requireNonNull(file.getContentType()));
        String keyName = subfolder + "/" + java.util.UUID.randomUUID() + "-" + originalFilename;
        try {
            s3Template.upload(bucketName, keyName, file.getInputStream());
            log.info("File '{}' uploaded to S3 in {} subfolder", originalFilename, subfolder);
            return new Blob(file.getOriginalFilename(), file.getContentType(), keyName, file.getSize());
        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            throw new StorageException(
                    StorageException.CloudStorageExceptionType.AWS_FAILED_TO_UPLOAD_FILE);
        }
    }

    @Override
    public List<Blob> uploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return List.of();
        }

        return Arrays.stream(files).filter(file -> !file.isEmpty()).map(this::uploadFile).toList();
    }

    /**
     * Deletes a file from the cloud storage
     *
     * @param objectKey the key of the file to delete
     */
    @Override
    public void deleteFile(String objectKey, String contentType) {
        if (objectKey == null || objectKey.isEmpty()) {
            log.warn("Delete attempt with null or empty URL.");
            return;
        }
        try {
            var bucketName = awsS3BucketProperties.s3().bucketName();

            log.info("Deleting S3 object: {}", objectKey);
            s3Template.deleteObject(bucketName, objectKey);
        } catch (Exception e) {
            log.error("Error during deletion process for URL {}: {}", objectKey, e.getMessage());
        }
    }


    /**
     * Generates a temporary, secure Pre-Signed URL for a private object in an AWS S3 bucket.
     *
     * <p>This method creates a short-lived, authenticated URL that grants temporary read-only access
     * to a specific S3 object. It packages the request details (bucket and object key) and signs them
     * using the application's secure IAM credentials. The resulting URL includes authentication
     * information in its query string and is valid only for a configured duration (e.g., 5 minutes).
     * This is the standard, secure way to allow clients to download private content from S3 without
     * exposing permanent links or secret credentials.
     *
     * @param objectKey   The full, unique key of the object in the S3 bucket (e.g.,
     *                    "files/uuid-filename.pdf").
     * @param contentType (Note: This parameter is currently unused in this implementation but is kept
     *                    for interface consistency).
     * @return A full, temporary pre-signed URL, or null if an error occurs.
     */
    @Override
    public String generatePresignedUrl(String objectKey, String contentType) {

        if (objectKey == null || objectKey.isEmpty()) {
            log.warn("Presigned URL generation attempted with null or empty object key.");
            return null;
        }

        try {

            // Step 1: Create a GetObjectRequest with the bucket name and object key
            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(accessUrlDurationInMinutes))
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(awsS3BucketProperties.s3().bucketName())
                                    .key(objectKey)
                                    .build())
                            .build();

            // Step 2: Use the S3Presigner to generate a presigned URL
            PresignedGetObjectRequest presignedGetObjectRequest =
                    s3Presigner.presignGetObject(presignRequest);

            return presignedGetObjectRequest.url().toString();

        } catch (Exception e) {
            log.error("Error generating presigned URL for object key {}: {}", objectKey,
                    e.getMessage());
        }
        return null;
    }
}
