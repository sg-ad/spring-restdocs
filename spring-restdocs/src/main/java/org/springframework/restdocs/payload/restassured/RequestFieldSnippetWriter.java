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
package org.springframework.restdocs.payload.restassured;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.FieldSnippetResultHandler;

/**
 * A {@link FieldSnippetResultHandler} for documenting a request's fields
 * 
 */
public class RequestFieldSnippetWriter extends FieldSnippetWriter {

	public RequestFieldSnippetWriter(String outputDir, List<FieldDescriptor> descriptors) {
		super(outputDir, "request", descriptors);
	}

	@Override
	protected Reader getPayloadReader(HttpRequest request, HttpResponse response, HttpContext context) throws IOException {
		if (request instanceof HttpEntityEnclosingRequest) {
			return new StringReader(EntityUtils.toString(((HttpEntityEnclosingRequest)request).getEntity()));
		} else {
			return new StringReader("");
		}
	}
}
