package com.example.datawarehouse.domain.service;

import com.example.datawarehouse.api.model.QueryRequest;
import com.fasterxml.jackson.databind.node.ArrayNode;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface QueryService {
	Flux<Map<String, Object>> queryRawAggregation(ArrayNode request);

	Flux<Map<String, Object>> query(QueryRequest request);
}
