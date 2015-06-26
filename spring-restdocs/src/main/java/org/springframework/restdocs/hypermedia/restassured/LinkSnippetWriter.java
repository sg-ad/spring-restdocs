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

package org.springframework.restdocs.hypermedia.restassured;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.restdocs.hypermedia.Link;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.hypermedia.LinkExtractor;
import org.springframework.restdocs.hypermedia.LinkExtractors;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.DocumentationWriter.TableAction;
import org.springframework.restdocs.snippet.DocumentationWriter.TableWriter;
import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;
import org.springframework.util.Assert;

/**
 * A {@link SnippetWritingResultHandler} that produces a snippet documenting a RESTful
 * resource's links.
 * 
 */
public class LinkSnippetWriter extends SnippetWriter {

	private final Map<String, LinkDescriptor> descriptorsByRel = new HashMap<String, LinkDescriptor>();

	private final LinkExtractor extractor;

	LinkSnippetWriter(String outputDir, LinkExtractor linkExtractor, List<LinkDescriptor> descriptors) {
		super(outputDir, "links");
		this.extractor = linkExtractor;
		for (LinkDescriptor descriptor : descriptors) {
			Assert.hasText(descriptor.getRel());
			Assert.hasText(descriptor.getDescription());
			this.descriptorsByRel.put(descriptor.getRel(), descriptor);
		}
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
		String responseContent = EntityUtils.toString(response.getEntity());
		String responseContentType = response.getFirstHeader("Content-Type").getName();
		writeLinks(writer, responseContent, responseContentType);
	}

	private void writeLinks(DocumentationWriter writer, String responseContent, String responseContentType) throws IOException {
		Map<String, List<Link>> links;
		if (this.extractor != null) {
			links = this.extractor.extractLinks(responseContent);
		}
		else {
			LinkExtractor extractorForContentType = LinkExtractors.extractorForContentType(responseContentType);
			if (extractorForContentType != null) {
				links = extractorForContentType.extractLinks(responseContent);
			}
			else {
				throw new IllegalStateException(
						"No LinkExtractor has been provided and one is not available for the content type "
								+ responseContentType);
			}

		}

		Set<String> actualRels = links.keySet();
		Set<String> expectedRels = this.descriptorsByRel.keySet();

		Set<String> undocumentedRels = new HashSet<String>(actualRels);
		undocumentedRels.removeAll(expectedRels);

		Set<String> missingRels = new HashSet<String>(expectedRels);
		missingRels.removeAll(actualRels);

		if (!undocumentedRels.isEmpty() || !missingRels.isEmpty()) {
			String message = "";
			if (!undocumentedRels.isEmpty()) {
				message += "Links with the following relations were not documented: "
						+ undocumentedRels;
			}
			if (!missingRels.isEmpty()) {
				message += "Links with the following relations were not found in the response: "
						+ missingRels;
			}
			fail(message);
		}

		Assert.isTrue(actualRels.equals(expectedRels));

		writer.table(new TableAction() {

			@Override
			public void perform(TableWriter tableWriter) throws IOException {
				tableWriter.headers("Relation", "Description");
				for (Entry<String, LinkDescriptor> entry : LinkSnippetWriter.this.descriptorsByRel
						.entrySet()) {
					tableWriter.row(entry.getKey(), entry.getValue().getDescription());
				}
			}

		});
	}

}
