package org.springframework.restdocs.http.restassured;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;

public abstract class ParamSnippetWriter extends SnippetWriter {

	private final List<ParamDescriptor> descriptors;

	public ParamSnippetWriter(String outputDir, String filename, List<ParamDescriptor> descriptors) {
		super(outputDir, filename + "-params");
		this.descriptors = descriptors;
	}
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
		writeFields(writer, request);
	}

	private void writeFields(DocumentationWriter writer, HttpRequest request) throws IOException {
		writer.table(tableWriter -> {
			tableWriter.headers(getParamColumnName(), "Description");
			for (ParamDescriptor descriptor : descriptors) {
				tableWriter.row(descriptor.getName(), descriptor.getDescription());
			}
		});
	}

	protected String getParamColumnName() {
		return "Parameter";
	}
}
