package org.springframework.restdocs.restassured;

public class DocumentingContextProvider {
	private static ThreadLocal<DocumentingContext> docContext = new ThreadLocal<>();
	
	public static void setContext(DocumentingContext context) {
		docContext.set(context);
	}
	
	public static DocumentingContext getContext() {
		return docContext.get();
	}
	
	public static void clear() {
		docContext.remove();
	}
}
