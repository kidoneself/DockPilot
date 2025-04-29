package com.dsm.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "docker")
public class DockerConfig {
    private String host;
    private Integer connectionTimeout;
    private Integer responseTimeout;
    private Integer maxConnections;

    @Bean
    public DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .build();

        ApacheDockerHttpClient.Builder httpClientBuilder = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(maxConnections)
                .connectionTimeout(Duration.ofMillis(connectionTimeout))
                .responseTimeout(Duration.ofMillis(responseTimeout));

        DockerHttpClient httpClient = httpClientBuilder.build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Integer responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }
} 