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

package org.springframework.restdocs.http.restassured;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.ccil.cowan.tagsoup.Parser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;
import org.springframework.restdocs.util.restassured.DocumentableHttpRequest;
import org.springframework.restdocs.util.restassured.DocumentableRequest;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.internal.path.json.JsonPrettifier;
import com.jayway.restassured.internal.path.xml.XmlPrettifier;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import groovy.util.XmlParser;

/**
 * Static factory methods for documenting a RESTful API's HTTP requests.
 */
public class HttpDocumentation {

	private HttpDocumentation() {

	}

	/**
	 * Produces a documentation snippet containing the request formatted as an HTTP
	 * request
	 * 
	 * @param outputDir The directory to which snippet should be written
	 * @return the handler that will produce the snippet
	 */
	public static SnippetWriter documentHttpRequest(String outputDir, String contentType) {
		String fileName = contentType == null ? "http-request" : "http-request-" + contentType.toLowerCase();
		return new SnippetWriter(outputDir, fileName) {
			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
				writer.codeBlock("http", new HttpRequestDocumentationAction(writer, request, response, context));
			}
		};
	}

	/**
	 * Produces a documentation snippet containing the response formatted as the HTTP
	 * response sent by the server
	 * 
	 * @param outputDir The directory to which snippet should be written
	 * @return the handler that will produce the snippet
	 */
	public static SnippetWriter documentHttpResponse(String outputDir, String contentType) {
		String fileName = contentType == null ? "http-response" : "http-response-" + contentType.toLowerCase();
		return new SnippetWriter(outputDir, fileName) {
			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
				writer.codeBlock("http", new HttpResponseDocumentationAction(writer, request, response, context));
			}
		};
	}

	private static class HttpRequestDocumentationAction implements DocumentationWriter.DocumentationAction {

		private DocumentationWriter writer;
		private FilterContext context;
		private FilterableRequestSpecification request;
		private Response response;
		private HttpRequest httpRequest;
		private HttpResponse httpResponse;
		private HttpContext httpContext;
		private URI url;

		HttpRequestDocumentationAction(DocumentationWriter writer,
									   FilterableRequestSpecification request,
									   Response response,
									   FilterContext context) {
			this.writer = writer;
			this.request = request;
			this.response = response;
			this.context = context;
		}

		HttpRequestDocumentationAction(DocumentationWriter writer, HttpRequest request, HttpResponse response, HttpContext context) throws URISyntaxException {
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
		public void perform() throws Exception {
			DocumentableRequest request = getDocumentableRequest();
			this.writer.printf("%s %s HTTP/1.1%n", request.getMethod(), request.getRequestUriWithQueryString());
			for (Entry<String, List<String>> header : request.getHeaders().entrySet()) {
				for (String value : header.getValue()) {
					this.writer.printf("%s: %s%n", header.getKey(), value);
				}
			}
			if (requiresFormEncodingContentType(request)) {
				this.writer.printf("%s: %s%n", HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
			}
			this.writer.println();
			if (request.getContentLength() > 0) {
				this.writer.println(request.getContentAsString(true));
			}
			else if (request.isPostRequest()) {
				String queryString = request.getParameterMapAsQueryString();
				if (StringUtils.hasText(queryString)) {
					this.writer.println(queryString);
				}
			}
		}

		protected DocumentableRequest getDocumentableRequest() throws MalformedURLException, URISyntaxException {
			return new DocumentableHttpRequest(this.httpRequest, this.httpContext);
		}

		private boolean requiresFormEncodingContentType(DocumentableRequest request) {
			return request.getHeaders().get(HttpHeaders.CONTENT_TYPE) == null
					&& request.isPostRequest()
					&& StringUtils.hasText(request.getParameterMapAsQueryString());
		}
	}

	private static final class HttpResponseDocumentationAction implements DocumentationWriter.DocumentationAction {

		private DocumentationWriter writer;
		private Response response;
		private HttpRequest httpRequest;
		private HttpResponse httpResponse;
		private HttpContext httpContext;
		private URI url;

		HttpResponseDocumentationAction(DocumentationWriter writer, HttpRequest request, HttpResponse response, HttpContext context) throws URISyntaxException {
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
		public void perform() throws IOException {
			HttpStatus status = HttpStatus.valueOf(getStatusCode());
			this.writer.println(String.format("HTTP/1.1 %d %s", status.value(), status.getReasonPhrase()));
			Map<String, List<String>> headers = getHeaders();
			for (String key : headers.keySet()) {
				for (String value : headers.get(key)) {
					this.writer.println(String.format("%s: %s", key, value));
				}
			}
			this.writer.println();
			this.writer.println(getBody());
		}

		private String getBody() throws IOException {
			if (httpResponse != null && httpResponse.getEntity() != null) {
				BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(httpResponse.getEntity());
				return getPrettyContent(bufferedHttpEntity); 
			} else if (response != null && response.getBody() != null) {
				return response.getBody().asString();
			}
			return "";
		}

		private String getPrettyContent(BufferedHttpEntity bufferedHttpEntity) throws IOException {
			String content = EntityUtils.toString(bufferedHttpEntity);
			try {
				ContentType contentType = ContentType.get(bufferedHttpEntity);
				if (contentType != null) {
					if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType())) {
						return JsonPrettifier.prettifyJson(content);
					} else if (ContentType.APPLICATION_XML.getMimeType().equals(contentType.getMimeType())) {
						return XmlPrettifier.prettify(new XmlParser(false, false), content);
					} else if (ContentType.TEXT_HTML.getMimeType().equals(contentType.getMimeType())) {
						return XmlPrettifier.prettify(new XmlParser(new Parser()), content);
					}
				}
			} catch (ParserConfigurationException | SAXException e) {
				// abandon attempt to prettify
			}
			return content;
		}

		private Map<String, List<String>> getHeaders() {
			Map<String, List<String>> headersByName = new HashMap<>();
			if (httpResponse != null) {
				for (org.apache.http.Header header : httpResponse.getAllHeaders()) {
					List<String> headerValues = headersByName.get(header.getName());
					if (headerValues == null) {
						headerValues = new ArrayList<>();
					}
					headerValues.add(header.getValue());
					headersByName.put(header.getName(), headerValues);
				}
			} else {
				for (Header header : response.getHeaders()) {
					List<String> headerValues = headersByName.get(header.getName());
					if (headerValues == null) {
						headerValues = new ArrayList<>();
					}
					headerValues.add(header.getValue());
					headersByName.put(header.getName(), headerValues);
				}
			}
			return headersByName;
		}

		private String getStatusLine() {
			if (httpResponse != null) {
				return httpResponse.getStatusLine().toString();
			} else {
				return response.statusLine();
			}
		}

		private int getStatusCode() {
			if (httpResponse != null) {
				return httpResponse.getStatusLine().getStatusCode();
			} else {
				return this.response.statusCode();
			}
		}
	}

}
