package com.oussema.cloud.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cloud.storage.aws")
public record AwsS3BucketProperties(S3 s3) {

    public record S3(
            @BucketExists @NotBlank(message = "S3 bucket name must be configured") String bucketName) {
    }

}
