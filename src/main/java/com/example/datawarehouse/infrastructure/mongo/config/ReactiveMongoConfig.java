package com.example.datawarehouse.infrastructure.mongo.config;

import com.mongodb.reactivestreams.client.MongoClient;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class ReactiveMongoConfig {
	private final MongoClient mongoClient;

	@Bean
	public ReactiveMongoTemplate reactiveMongoTemplate(@Value("${spring.data.mongodb.database}") String databaseName) {
		return new ReactiveMongoTemplate(mongoClient, databaseName);
	}
}
