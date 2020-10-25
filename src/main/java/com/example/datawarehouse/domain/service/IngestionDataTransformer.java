package com.example.datawarehouse.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class IngestionDataTransformer {
	private static final DateTimeFormatter AMERICAN_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy");
	public static final String FILENAME = "filename";

	private static final ConcurrentHashMap<String, Function<String, Optional<Object>>> handlersCache = new ConcurrentHashMap<>();

	static Map<String, Object> transform(String fileUriPath, String[] headersVal, String[] rowVal) {
		final Map<String, Object> document = new HashMap<>();
		document.put(FILENAME, fileUriPath);
		for (int i = 0; i < headersVal.length; i++) {
			final String header = headersVal[i];
			final String row = rowVal[i];
			document.put(header, guessDataType(header, row));
		}
		return document;
	}

	public static Object guessDataType(String header, String value) {
		Optional<Object> convertedData = handlersCache
				.computeIfAbsent(header, s -> {
					final Optional<Long> number = getNumber(value);
					if (number.isPresent()) {
						return val -> getNumber(val).map(aLong -> aLong);
					} else {
						final Optional<LocalDate> date = getDate(value);
						if (date.isPresent()) {
							return val -> getDate(val).map(localDate -> localDate);
						} else {
							return Optional::of;
						}
					}
				})
				.apply(value);

		return convertedData.orElse(value);
	}

	private static Optional<LocalDate> getDate(String value) {
		try {
			return Optional.of(LocalDate.from(AMERICAN_DATE_FORMAT.parse(value)));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private static Optional<Long> getNumber(String value) {
		try {
			return Optional.of(Long.parseLong(value));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
