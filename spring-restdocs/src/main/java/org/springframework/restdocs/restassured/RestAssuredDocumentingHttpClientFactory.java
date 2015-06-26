package org.springframework.restdocs.restassured;

import org.apache.http.client.HttpClient;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;

import com.jayway.restassured.config.HttpClientConfig;

public class RestAssuredDocumentingHttpClientFactory extends HttpClientConfig.HttpClientFactory {
	@Override
	public HttpClient createHttpClient() {
		HttpClientConfig.httpClientConfig().httpClientInstance();
		DefaultHttpClient client = new DefaultHttpClient();
		client.addRequestInterceptor((request, ctx) -> {
			DocumentingContext docContext = DocumentingContextProvider.getContext();
			if (docContext != null) {
				for (SnippetWriter handler : docContext.getRequestHandlers()) {
					try {
						handler.handle(request, null, ctx);
					} catch (Exception e) {
						// not much to do; log error
					}
				}
			}
		});
		client.addResponseInterceptor((response, ctx) -> {
			DocumentingContext docContext = DocumentingContextProvider.getContext();
			if (docContext != null) {
				// make the response's content readable multiple times if it isn't already
				if (response.getEntity() != null && !response.getEntity().isRepeatable()) {
					response.setEntity(new BufferedHttpEntity(response.getEntity()));
				}
				for (SnippetWriter handler : docContext.getResponseHandlers()) {
					try {
						handler.handle(null, response, ctx);
					} catch (Exception e) {
						// not much to do; log error
					}
				}
			}
			DocumentingContextProvider.clear();
		});
		return client;
	}
}
