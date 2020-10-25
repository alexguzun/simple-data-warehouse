package com.example.datawarehouse.api.model;

import com.example.datawarehouse.domain.model.querydsl.AggregateOperators;
import com.example.datawarehouse.domain.model.querydsl.FilterOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class QueryRequest {

	@Schema(name = "filter", description = "an optional set of dimension filters to be filtered on")
	private Map<String, Map<FilterOperator, String>> filter;

	@Schema(name = "groupBy", description = "an optional set of dimensions to be grouped by")
	private Set<String> groupBy;

	@Schema(name = "aggregate", description = "a set of metrics to be aggregated on")
	private Map<String, AggregateOperators> aggregate;
}
