package com.oussema.cloud.multicloud;

import com.oussema.cloud.config.AwsS3BucketProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(AwsS3BucketProperties.class)
public class MultiCloudConfig {
}