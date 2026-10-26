package com.oussema.cloud.multicloud;

import com.azure.storage.blob.BlobServiceClient;
import com.oussema.cloud.adapter.Storage;
import com.oussema.cloud.exception.StorageException;
import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class CloudRouter {

    private record Health(boolean ok, long checkedAt) {}

    private static final long CACHE_MS = 3000;

    private final Storage aws;
    private final Storage azure;
    private final S3Template s3Template;
    private final BlobServiceClient blobServiceClient;
    private final String awsBucket;
    private final Map<Cloud, Health> health = new ConcurrentHashMap<>();

    public CloudRouter(@Qualifier("awsStorageAdapter") Storage aws,
                       @Qualifier("azureStorageAdapter") Storage azure,
                       S3Template s3Template,
                       BlobServiceClient blobServiceClient,
                       @Value("${cloud.storage.aws.s3.bucket-name}") String awsBucket) {
        this.aws = aws;
        this.azure = azure;
        this.s3Template = s3Template;
        this.blobServiceClient = blobServiceClient;
        this.awsBucket = awsBucket;
    }

    public Storage storage(Cloud cloud) {
        return cloud == Cloud.AWS ? aws : azure;
    }

    public String presignedUrl(Cloud cloud, String key, String contentType) {
        return storage(cloud).generatePresignedUrl(key, contentType);
    }

    /** Use the preferred cloud if it is reachable, otherwise the other one. */
    public Cloud chooseForUpload(Cloud preferred) {
        if (isHealthy(preferred)) return preferred;
        if (isHealthy(preferred.other())) return preferred.other();
        throw new StorageException(StorageException.CloudStorageExceptionType.FILE_UPLOAD_FAILED,
                "No cloud storage is reachable");
    }

    public boolean isHealthy(Cloud cloud) {
        long now = System.currentTimeMillis();
        Health h = health.get(cloud);
        if (h != null && now - h.checkedAt() < CACHE_MS) return h.ok();
        boolean ok = probe(cloud);
        health.put(cloud, new Health(ok, now));
        return ok;
    }

    // Bounded to 2 seconds so a stopped cloud never makes the API hang
    private boolean probe(Cloud cloud) {
        try {
            return CompletableFuture.supplyAsync(() -> switch (cloud) {
                case AWS -> s3Template.bucketExists(awsBucket);
                case AZURE -> blobServiceClient.getBlobContainerClient("files").exists();
            }).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }
}