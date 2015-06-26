package org.springframework.restdocs.restassured;

import java.util.ArrayList;
import java.util.List;

import org.springframework.restdocs.snippet.restassured.SnippetWriter;

public class DocumentingContext {
	private final String outputDir;

	private List<SnippetWriter> requestHandlers;
	private List<SnippetWriter> responseHandlers;

	DocumentingContext(String outputDir) {
		this.outputDir = outputDir;
		this.requestHandlers = new ArrayList<>();
		this.responseHandlers = new ArrayList<>();
	}
	
	public List<SnippetWriter> getRequestHandlers() {
		return requestHandlers;
	}
	
	public List<SnippetWriter> getResponseHandlers() {
		return responseHandlers;
	}
	
}
