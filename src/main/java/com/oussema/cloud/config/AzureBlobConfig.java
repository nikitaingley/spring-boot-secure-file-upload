package com.oussema.cloud.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cloud.storage.provider", havingValue = "azure")
public class AzureBlobConfig {

    @Bean
    @ConditionalOnMissingBean
    public BlobServiceClient blobServiceClient(
            @Value("${cloud.azure.storage.blob.connection-string}") String connectionString) {
        BlobServiceClient client = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        // Azurite (and a brand-new Azure storage account) starts with no containers.
        // The adapter uploads to the "files" container, so make sure it exists.
        client.getBlobContainerClient("files").createIfNotExists();
        client.getBlobContainerClient("images").createIfNotExists();

        return client;
    }
}