package com.oussema.cloud.adapter;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.oussema.cloud.exception.StorageException;
import com.oussema.cloud.model.Blob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter class for Azure cloud storage, with logic aligned to the AWS adapter.
 */
@Component
@Log4j2
@ConditionalOnProperty(name = "cloud.storage.provider", havingValue = "azure")
@RequiredArgsConstructor
public class AzureStorageAdapter implements Storage {

    private static final String IMAGES_CONTAINER = "images";
    private static final String FILES_CONTAINER = "files";
    private final BlobServiceClient blobServiceClient;

    @Value("${cloud.storage.access-url-duration-minutes:30}")
    private long accessUrlDurationInMinutes;


    @PostConstruct
    public void logActiveAdapter() {
        log.info("Azure Adapter is active");
    }


    /**
     * Determines the appropriate container based on file content type.
     *
     * @param contentType the MIME type of the file.
     * @return the name of the container where the file should be stored.
     */
    private String determineContainerForContentType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return IMAGES_CONTAINER;
        }
        return FILES_CONTAINER;
    }

    /**
     * Method to get the blob client for a blob.
     *
     * @param blobName      the name of the blob.
     * @param containerName the name of the container.
     * @return the blob client.
     */
    private BlockBlobClient getBlobClient(String blobName, String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        return containerClient.getBlobClient(blobName).getBlockBlobClient();
    }

    /**
     * Uploads a file to the appropriate Azure container based on its content type.
     *
     * @param file the file to upload.
     * @return a Blob object with details of the uploaded file.
     */
    @Override
    public Blob uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload attempt with null or empty file.");
            return null;
        }

        String contentType = file.getContentType();
        String containerName = determineContainerForContentType(contentType);

        // Validate file size based on type

        String blobName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        BlockBlobClient blobClient = getBlobClient(blobName, containerName);

        try {
            byte[] bytes = file.getBytes();
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                // Upload the file, overwriting if it already exists
                blobClient.upload(inputStream, bytes.length, true);
                BlobHttpHeaders headers = new BlobHttpHeaders();
                headers.setContentType(contentType);
                blobClient.setHttpHeaders(headers);
            }
            log.info(
                    "File '{}' uploaded to Azure in {} container", file.getOriginalFilename(), containerName);
            return new Blob(
                    file.getOriginalFilename(), contentType, blobClient.getBlobName(), file.getSize());
        } catch (Exception e) {
            log.error("Failed to upload file to Azure", e);
            throw new StorageException(
                    StorageException.CloudStorageExceptionType.AZURE_FAILED_TO_UPLOAD_FILE);
        }
    }

    /**
     * Uploads multiple files to cloud storage.
     *
     * @param files array of files to upload.
     * @return a list of Blob objects for the uploaded files.
     */
    @Override
    public List<Blob> uploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return List.of();
        }

        return Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .map(this::uploadFile)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Deletes a file from Azure storage by its URL. It determines the correct container ('images' or
     * 'files') from the URL structure.
     *
     * @param blobName the full name of the file to delete.
     */
    @Override
    public void deleteFile(String blobName, String contentType) {
        if (blobName == null || blobName.isEmpty()) {
            log.warn("Delete attempt with null or empty URL.");
            return;
        }

        String containerName = determineContainerForContentType(contentType);

        try {
            BlockBlobClient blobClient = getBlobClient(blobName, containerName);
            blobClient.deleteIfExists();
            log.info("Deleted Azure blob: {} from container: {}", blobName, containerName);
        } catch (Exception e) {
            log.error(
                    "Error during Azure blob deletion process for URL {}: {}", blobName, e.getMessage(), e);
        }
    }

    /**
     * Generates a temporary, secure Shared Access Signature (SAS) URL for a private blob in Azure
     * Blob Storage.
     *
     * <p>This method acts as a secure gatekeeper. Instead of exposing a permanent link to a file, it
     * creates a short-lived URL with a cryptographic signature. This URL grants temporary read-only
     * access to a specific blob, determined by its name and content type (which dictates the
     * container). The URL automatically expires after a configured duration (e.g., 5 minutes),
     * ensuring that access is time-limited and controlled.
     *
     * @param blobName    The unique name of the blob within its container (e.g., "uuid-filename.pdf").
     * @param contentType The MIME type of the file (e.g., "application/pdf"), used to determine the
     *                    correct storage container ("files" or "images").
     * @return A full, temporary URL with a SAS token appended, or null if an error occurs.
     */
    @Override
    public String generatePresignedUrl(String blobName, String contentType) {
        if (blobName == null || blobName.isEmpty()) {
            log.warn("SAS URL generation attempted with null or empty blob name.");
            return null;
        }

        try {

            String containerName = determineContainerForContentType(contentType);

            BlobClient blobClient =
                    blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName);

            BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);

            OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(accessUrlDurationInMinutes);

            // Build the signature values
            BlobServiceSasSignatureValues signatureValues =
                    new BlobServiceSasSignatureValues(expiryTime, sasPermission);

            String sasToken = blobClient.generateSas(signatureValues);

            return String.format("%s?%s", blobClient.getBlobUrl(), sasToken);

        } catch (Exception e) {
            log.error("Error generating SAS URL for blob name {}: {}", blobName, e.getMessage());
            return null;
        }
    }
}
