package org.springframework.restdocs.restassured;

public class RestAssuredDocumentation {
	private RestAssuredDocumentation() {
	}

	public static RestAssuredDocumentFilter document(String outputDir) {
		return document(outputDir, null);
	}
	
	public static RestAssuredDocumentFilter document(String outputDir, String contentType) {
		DocumentingContextProvider.setContext(new DocumentingContext(outputDir));
		return new RestAssuredDocumentFilter(outputDir, contentType);
	}
}
