package com.nhnacademy.environment.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.QueryApi;
import com.nhnacademy.environment.property.InfluxDbProperties;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbConfig {

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient(InfluxDbProperties properties) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.writeTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .callTimeout(properties.callTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true);

        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(properties.url())
                .authenticateToken(properties.token().toCharArray())
                .org(properties.org())
                .clientType(properties.clientType())
                .logLevel(properties.logLevel())
                .okHttpClient(okHttpClientBuilder)
                .build();

        return InfluxDBClientFactory.create(options).enableGzip();
    }

    @Bean
    public QueryApi queryApi(InfluxDBClient influxDBClient) {
        return influxDBClient.getQueryApi();
    }
}
