package org.springframework.restdocs.http.restassured;

import java.util.List;

public class PathParameterSnippetWriter extends ParamSnippetWriter {
	public PathParameterSnippetWriter(String outputDir, List<ParamDescriptor> descriptors) {
		super(outputDir, "request-path", descriptors);
	}

	@Override
	protected String getParamColumnName() {
		return "Request Parameter";
	}
}
