package org.springframework.restdocs.http.restassured;

import java.util.List;

public class QueryParameterSnippetWriter extends ParamSnippetWriter {
	public QueryParameterSnippetWriter(String outputDir, List<ParamDescriptor> descriptors) {
		super(outputDir, "request-query", descriptors);
	}

	@Override
	protected String getParamColumnName() {
		return "Query Parameter";
	}
}
