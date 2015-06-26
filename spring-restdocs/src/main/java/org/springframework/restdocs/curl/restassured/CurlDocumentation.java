/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.restdocs.curl.restassured;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.DocumentationWriter.DocumentationAction;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;
import org.springframework.restdocs.util.restassured.DocumentableHttpRequest;
import org.springframework.restdocs.util.restassured.DocumentableRequest;
import org.springframework.util.StringUtils;

/**
 * Static factory methods for documenting a RESTful API as if it were being driven using
 * the cURL command-line utility.
 *
 */
public abstract class CurlDocumentation {

	private CurlDocumentation() {

	}

	/**
	 * Produces a documentation snippet containing the request formatted as a cURL command
	 *
	 * @param outputDir The directory to which snippet should be written
	 * @return the handler that will produce the snippet
	 */
	public static SnippetWriter documentCurlRequest(String outputDir, String contentType) {
		String fileName = contentType == null ? "curl-request" : "curl-request-" + contentType.toLowerCase();
		return new SnippetWriter(outputDir, fileName) {
			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
				writer.shellCommand(new CurlRequestDocumentationAction(writer, request, response, context));
			}
		};
	}

	private static final class CurlRequestDocumentationAction implements DocumentationAction {

		private final DocumentationWriter writer;
		
		private URI url;
		private HttpRequest httpRequest;
		private HttpResponse httpResponse;
		private HttpContext httpContext;
		
		CurlRequestDocumentationAction(DocumentationWriter writer,
									   HttpRequest request,
									   HttpResponse response,
									   HttpContext context) throws URISyntaxException {
			this.writer = writer;
			this.httpRequest = request;
			this.httpResponse = response;
			this.httpContext = context;
			if (httpRequest != null) {
				HttpHost host = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
				this.url = new URI(host.toURI()).resolve(request.getRequestLine().getUri());
			}
		}

		@Override
		public void perform() throws IOException, URISyntaxException {
			DocumentableRequest request = getDocumentableRequest();
			this.writer.print("curl ");
			this.writer.print(request.getRequestUriWithQueryString().replace("&", "\\&"));

			this.writer.print(" -i");

			if (!request.isGetRequest()) {
				this.writer.print(String.format(" -X %s", request.getMethod()));
			}

			for (Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
				for (String header : entry.getValue()) {
					this.writer.print(String.format(" -H \"%s: %s\"", entry.getKey(), header));
				}
			}

			if (request.getContentLength() > 0) {
				this.writer.print(String.format(" -d '%s'", request.getContentAsString(true)));
			}
			else if (request.isPostRequest()) {
				String queryString = request.getParameterMapAsQueryString();
				if (StringUtils.hasText(queryString)) {
					this.writer.print(String.format(" -d '%s'",
							queryString.replace("&", "\\&")));
				}
			}

			this.writer.println();
		}

		private DocumentableRequest getDocumentableRequest() throws MalformedURLException, URISyntaxException {
			return new DocumentableHttpRequest(this.httpRequest, this.httpContext);
		}
	}
}
