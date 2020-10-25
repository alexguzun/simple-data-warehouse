package com.example.datawarehouse.infrastructure.mongo;

import com.example.datawarehouse.domain.repository.DataRepository;
import com.example.datawarehouse.domain.config.DataWarehouseConfig;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Service
@Slf4j
public class MongoDataRepository implements DataRepository {
	private static final String FILENAME = "filename";
	private final ReactiveMongoTemplate mongoTemplate;
	private final DataWarehouseConfig dataWarehouseConfig;

	@Autowired
	public MongoDataRepository(ReactiveMongoTemplate mongoTemplate, DataWarehouseConfig dataWarehouseConfig) {
		this.mongoTemplate = mongoTemplate;
		this.dataWarehouseConfig = dataWarehouseConfig;
	}

	@Override
	public Mono<MongoCollection<Document>> getDataCollection() {
		return this.mongoTemplate.getCollection(this.dataWarehouseConfig.getDbName());
	}

	/**
	 * Delete all data that was previously ingested from the file.
	 *
	 * @param filename from witch data was ingested
	 * @return {@link Mono} that will complete when the delete has ben performed
	 */
	@Override
	public Mono<Void> deleteIngestedData(String filename) {
		return getDataCollection()
				.flatMap(collection -> {
					//Delete all entries that were previously ingested
					return Mono.from(collection.deleteMany(eq(FILENAME, filename)))
							.doOnNext(deleteResult -> {
								if (deleteResult.getDeletedCount() > 0) {
									log.debug("Deleted {} previously imported entries for file {}", deleteResult.getDeletedCount(), filename);
								}
							});
				})
				.then();
	}

	/**
	 * Insert a batch of data entries.
	 *
	 * @param data data entries
	 * @return {@link Mono} that will complete with a Boolean if data was saved successfully
	 */
	@Override
	public Mono<Boolean> insertMany(List<Map<String, Object>> data) {
		final List<Document> documents = data.stream()
				.map(Document::new)
				.collect(Collectors.toList());

		return getDataCollection()
				.flatMap(collection -> {
					return Mono.from(collection.insertMany(documents))
							.map(InsertManyResult::wasAcknowledged);
				});
	}

	@Override
	public Flux<Map<String, Object>> aggregate(List<Bson> stages) {
		return getDataCollection()
				.flux()
				.flatMap(collection -> {
					return Flux.from(collection.aggregate(stages));
				})
				.map(this::toMap);
	}

	private Map<String, Object> toMap(Document document) {
		return document.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
