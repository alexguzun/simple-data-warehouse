package com.example.datawarehouse.domain.service;

import com.example.datawarehouse.domain.repository.DataRepository;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Service
@Slf4j
public class IngestionService {

	private final WebClient webClient;
	private final DataRepository dataRepository;

	@Autowired
	public IngestionService(WebClient.Builder webClientBuilder, DataRepository dataRepository) {
		this.webClient = webClientBuilder.build();
		this.dataRepository = dataRepository;
	}

	public Mono<Boolean> ingestFile(URI fileUri) {
		log.debug("Starting ingestion of file {}", fileUri);
		final String filename = fileUri.getPath();

		var csvFile = webClient.get()
				.uri(fileUri)
				.retrieve()
				.bodyToFlux(String.class)
				.retryWhen(Retry.backoff(5, Duration.ofMillis(500))
						.doBeforeRetry(retrySignal -> {
							log.info("Failed to retrieve file {} because of {} . Will retry", fileUri, retrySignal.failure().getMessage());
						})
				);

		return saveCsvFile(filename, csvFile)
				.doOnNext(result -> {
					if (result) {
						log.info("Successfully ingested file {}", fileUri);
					} else {
						log.info("Failed to ingest file {}. See previous errors.", fileUri);
					}
				});
	}

	public Mono<Boolean> saveCsvFile(String filename, Flux<String> csvFileStream) {
		final Mono<Void> deleteIngested = dataRepository.deleteIngestedData(filename);

		final var headers = csvFileStream
				.take(1) // headers row
				.map(firstRow -> firstRow.split(","))
				.single();

		//Create indexes based on headers and one filename
		headers.flatMap(fields -> dataRepository.getDataCollection()
				.flatMap(collection -> {
					IndexOptions indexOptions = new IndexOptions().name(filename + "_index");
					return Mono.from(collection.createIndex(Indexes.ascending(fields), indexOptions))
							.then(Mono.from(collection.createIndex(Indexes.ascending(IngestionDataTransformer.FILENAME))))
							.onErrorContinue((throwable, o) -> {
								log.error("Failed to create index for file {}", filename, throwable);
							});
				})
		).subscribe();

		return deleteIngested.thenMany(csvFileStream)
				.skip(1) //skip the headers row
				.map(row -> row.split(","))
				.flatMap(rowVal -> {
					return headers.map(headersVal -> {
						return IngestionDataTransformer.transform(filename, headersVal, rowVal);
					});
				})
				.buffer(50)
				.flatMap(dataRepository::insertMany)
				.reduce(Boolean.TRUE, (initial, result) -> initial && result);
	}

}
