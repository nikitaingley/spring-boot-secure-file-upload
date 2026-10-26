package com.oussema.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

@Configuration
public class ClamavConfig {


    @Value("${clamav.host}")
    private String host;

    @Value("${clamav.port}")
    private int port;

    @Bean
    public ClamavClient clamavClient() {
        return new ClamavClient(host, port);
    }
}
