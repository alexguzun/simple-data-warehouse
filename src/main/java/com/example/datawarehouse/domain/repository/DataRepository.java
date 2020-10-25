package com.example.datawarehouse.domain.repository;

import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DataRepository {
	Mono<MongoCollection<Document>> getDataCollection();

	Mono<Void> deleteIngestedData(String filename);

	Mono<Boolean> insertMany(List<Map<String, Object>> data);

	Flux<Map<String, Object>> aggregate(List<Bson> stages);
}
