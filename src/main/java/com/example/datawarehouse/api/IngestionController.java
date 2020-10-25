package com.example.datawarehouse.api;

import com.example.datawarehouse.api.model.IngestRequest;
import com.example.datawarehouse.domain.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping(value = "/ingest")
@Slf4j
public class IngestionController {

	private static final String CSV_EXTENSION = ".csv";
	private final IngestionService ingestionService;

	@Autowired
	public IngestionController(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Operation(description = "Ingest a CSV file from a URL")
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<Object>> ingest(@Valid @RequestBody IngestRequest request) {
		final var fileUriPath = request.getUrl().getPath();
		if (fileUriPath.contains(".")) {
			final var extension = fileUriPath.substring(fileUriPath.lastIndexOf("."));
			if (extension.equalsIgnoreCase(CSV_EXTENSION)) {
				return this.ingestionService
						.ingestFile(request.getUrl())
						.map(unused -> ResponseEntity.ok().build());
			} else {
				return badRequest("File extension not supported for ingestion");
			}
		} else {
			return badRequest("Could not identify file extension");
		}
	}

	private Mono<ResponseEntity<Object>> badRequest(String errorMessage) {
		return Mono.just(ResponseEntity
				.badRequest()
				.body(Map.of("error", errorMessage))
		);
	}
}
