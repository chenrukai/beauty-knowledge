package com.beauty.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    public static final int VECTOR_DIM = 512;
    public static final String COLLECTION_NAME = "knowledge_vectors";

    @Value("${beauty.milvus.host}")
    private String host;

    @Value("${beauty.milvus.port}")
    private Integer port;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        return new MilvusServiceClient(connectParam);
    }
}
