package com.example.datawarehouse.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.net.URI;

@Data
@NoArgsConstructor
public class IngestRequest {
	@NotNull
	private URI url;
}
