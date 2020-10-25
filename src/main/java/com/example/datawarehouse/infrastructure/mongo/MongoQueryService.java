package com.example.datawarehouse.infrastructure.mongo;

import com.example.datawarehouse.api.model.QueryRequest;
import com.example.datawarehouse.domain.repository.DataRepository;
import com.example.datawarehouse.domain.model.querydsl.FilterOperator;
import com.example.datawarehouse.domain.service.IngestionDataTransformer;
import com.example.datawarehouse.domain.service.QueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.model.Aggregates;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

@Service
@Slf4j
public class MongoQueryService implements QueryService {
	private final DataRepository repository;

	@Autowired
	public MongoQueryService(DataRepository repository) {
		this.repository = repository;
	}

	@Override
	public Flux<Map<String, Object>> queryRawAggregation(ArrayNode request) {
		List<Bson> stages = new ArrayList<>();
		for (JsonNode jsonNode : request) {
			try {
				stages.add(Document.parse(jsonNode.toString()));
			} catch (Exception e) {
				log.error("Failed to parse request ", e);
				throw new IllegalArgumentException(e.getMessage());
			}
		}

		return repository.aggregate(stages);
	}

	@Override
	public Flux<Map<String, Object>> query(QueryRequest request) {
		final Optional<Bson> matchStage = buildMatchStage(request);
		final Optional<Bson> groupByStage = buildGroupByStage(request);
		final Optional<Bson> projectStage = getProjectionStage(request);

		final List<Bson> stages = new ArrayList<>();
		matchStage.ifPresent(stages::add);
		groupByStage.ifPresent(stages::add);
		projectStage.ifPresent(stages::add);

		return repository.aggregate(stages);
	}

	private Optional<Bson> buildMatchStage(QueryRequest request) {
		final Map<String, Map<FilterOperator, String>> filter = request.getFilter();
		if (filter != null && !filter.isEmpty()) {
			final Map<String, Object> collect = filter.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, fieldEntry -> {
						final Document fieldFilters = new Document();

						fieldEntry.getValue().forEach((filterOperator, value) -> {
							fieldFilters.append("$" + filterOperator, IngestionDataTransformer.guessDataType(fieldEntry.getKey(), value));
						});

						return fieldFilters;
					}));

			return Optional.of(new Document("$match", collect));
		} else {
			return Optional.empty();
		}
	}

	private Optional<Bson> buildGroupByStage(QueryRequest request) {
		if (request.getGroupBy() == null && request.getAggregate() == null) {
			return Optional.empty();
		} else {
			Map<String, String> groupById = new HashMap<>();
			final Document groupByDoc;
			if (request.getGroupBy() != null) {
				request.getGroupBy().forEach(s -> {
					groupById.put(s, "$" + s);
				});
				groupByDoc = new Document("_id", groupById);
			} else {
				groupByDoc = new Document("_id", "null");
			}

			if (request.getAggregate() != null) {
				request.getAggregate().forEach((key, aggregateOperation) -> {
					groupByDoc.append(key, new Document("$" + aggregateOperation, "$" + key));
				});
			}

			return Optional.of(new Document("$group", groupByDoc));
		}
	}

	private Optional<Bson> getProjectionStage(QueryRequest request) {
		List<Bson> projectionIncludeGroupId = new ArrayList<>();
		if (request.getGroupBy() != null) {
			request.getGroupBy().forEach(groupBy -> {
				projectionIncludeGroupId.add(computed(groupBy, "$_id." + groupBy));
			});
		}

		final ArrayList<Bson> projectionFields = new ArrayList<>();
		if (request.getAggregate() != null) {
			request.getAggregate().keySet().forEach(s -> projectionFields.add(include(s)));
		}
		if (!projectionIncludeGroupId.isEmpty()) {
			projectionFields.add(excludeId());
			projectionFields.addAll(projectionIncludeGroupId);
			return Optional.of(Aggregates.project(fields(projectionFields)));
		} else {
			return Optional.empty();
		}
	}
}
