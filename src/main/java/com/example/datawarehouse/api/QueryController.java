package com.example.datawarehouse.api;

import com.example.datawarehouse.api.model.QueryRequest;
import com.example.datawarehouse.domain.service.QueryService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.MongoCommandException;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.util.Map;


@RestController
@RequestMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class QueryController {
	private final QueryService queryService;

	@Autowired
	public QueryController(QueryService queryService) {
		this.queryService = queryService;
	}

	@Operation(description = "Execute a query using request DSL")
	@RequestMapping(method = RequestMethod.POST)
	public Flux<Map<String, Object>> query(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Query Json", required = true,
					content = @Content(examples = @ExampleObject(value = "{ \"filter\": { \"Datasource\": { \"eq\": \"Google Ads\" } }, \"groupBy\": [ \"Campaign\" ], \"aggregate\": { \"Clicks\": \"sum\" } }")))
			@Valid @RequestBody QueryRequest request) {
		return queryService.query(request);
	}

	@Operation(description = "Execute a query as a MongoDB Aggregation JSON",
			externalDocs = @ExternalDocumentation(description = "MongoDB Aggregation Spec", url = "https://docs.mongodb.com/manual/reference/operator/aggregation-pipeline/")
	)
	@RequestMapping(value = "/raw", method = RequestMethod.POST)
	public Flux<Map<String, Object>> queryRaw(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Aggregation json", required = true,
					content = @Content(examples = @ExampleObject(value = "[ { \"$match\": { \"Datasource\": \"Google Ads\" } }, { \"$group\": { \"_id\": { \"Campaign\": \"$Campaign\", \"Datasource\": \"$Datasource\" }, \"result\": { \"$sum\": \"$Clicks\" } } } ]")))
			@RequestBody ArrayNode request) {
		if (request.size() == 0) {
			throw new IllegalArgumentException("Missing raw aggregation query");
		}
		return queryService.queryRawAggregation(request);
	}

	@ExceptionHandler
	public ResponseEntity<Map<String, String>> handle(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler
	public ResponseEntity<Map<String, String>> handle(MongoCommandException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
	}

}
