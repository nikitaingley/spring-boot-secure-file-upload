package com.oussema.cloud;

import com.oussema.cloud.config.AwsS3BucketProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AwsS3BucketProperties.class)
public class CloudPresignApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPresignApplication.class, args);
    }

}
