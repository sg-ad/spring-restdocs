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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.FieldExtractor;
import org.springframework.restdocs.payload.FieldType;
import org.springframework.restdocs.payload.FieldTypeResolver;
import org.springframework.restdocs.payload.FieldValidator;
import org.springframework.restdocs.snippet.AsciidoctorWriter;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.DocumentationWriter.TableAction;
import org.springframework.restdocs.snippet.DocumentationWriter.TableWriter;
import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.restdocs.snippet.restassured.SnippetWriter;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link SnippetWritingResultHandler} that produces a snippet documenting a RESTful
 * resource's request or response fields.
 * 
 */
public abstract class FieldSnippetWriter extends SnippetWriter {

	private final Map<String, FieldDescriptor> descriptorsByPath = new LinkedHashMap<String, FieldDescriptor>();
	
	

	private final FieldTypeResolver fieldTypeResolver = new FieldTypeResolver();

	private final FieldExtractor fieldExtractor = new FieldExtractor();

	private final FieldValidator fieldValidator = new FieldValidator();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private List<FieldDescriptor> fieldDescriptors;

	FieldSnippetWriter(String outputDir, String filename,
					   List<FieldDescriptor> descriptors) {
		super(outputDir, filename + "-fields");
		for (FieldDescriptor descriptor : descriptors) {
			Assert.notNull(descriptor.getPath());
			Assert.hasText(descriptor.getDescription());
			this.descriptorsByPath.put(descriptor.getPath(), descriptor);
		}
		this.fieldDescriptors = descriptors;
	}

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception {
		// don't reuse payload reader
		validateFields(getPayloadReader(request, response, context));
		writeFields(writer, getPayloadReader(request, response, context));
	}

	private void writeFields(DocumentationWriter writer, Reader payloadReader) throws IOException {

		final Map<String, Object> payload = extractPayload(payloadReader);
		
		List<String> missingFields = new ArrayList<String>();

		findMissingFields(payload, missingFields, this.fieldDescriptors, "");

		writeTable(writer, payload, fieldDescriptors);
	}

	private void writeTable(DocumentationWriter writer, final Map<String, Object> payload, final List<FieldDescriptor> descriptors) throws IOException {
		writer.table(new MyTableAction(descriptors, payload));
	}

	private void findMissingFields(Map<String, Object> payload, List<String> missingFields, List<FieldDescriptor> fieldDescriptors, String rootPath) {
		for (FieldDescriptor fieldDescriptor : fieldDescriptors) {
			String fieldPath = StringUtils.isNotBlank(rootPath) ? rootPath + "." + fieldDescriptor.getPath() : fieldDescriptor.getPath();
			if (fieldDescriptor.hasChild()) {
				findMissingFields(payload, missingFields, fieldDescriptor.getChildren(), fieldPath);
			} else if (!fieldDescriptor.isOptional()) {
				Object field = this.fieldExtractor.extractField(fieldPath, payload);
				if (field == null) {
					missingFields.add(fieldPath);
				}
			}
		}
	}

	private void validateFields(Reader payloadReader) throws IOException {
		this.fieldValidator.validate(payloadReader, this.fieldDescriptors);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractPayload(Reader payloadReader) throws IOException {
		try {
			return this.objectMapper.readValue(payloadReader, Map.class);
		}
		finally {
			payloadReader.close();
		}
	}

	protected abstract Reader getPayloadReader(HttpRequest request, HttpResponse response, HttpContext context) throws IOException;

	private class MyTableAction implements TableAction {
		private final String rootPath;
		private final List<FieldDescriptor> descriptors;
		private final Map<String, Object> payload;

		public MyTableAction(List<FieldDescriptor> descriptors, Map<String, Object> payload) {
			this("", descriptors, payload);
		}

		public MyTableAction(String rootPath, List<FieldDescriptor> descriptors, Map<String, Object> payload) {
			this.rootPath = rootPath;
			this.descriptors = descriptors;
			this.payload = payload;
		}

		@Override
		public void perform(TableWriter tableWriter) throws IOException {
			tableWriter.headers("Attribute", "Type", "Description");
			for (FieldDescriptor entry : descriptors) {
				if (entry.hasChild()) {
					String filePrefix = StringUtils.isNotBlank(rootPath) ? fileName + "-" + rootPath : fileName;
					File newFile = resolveFile(outputDir, filePrefix + "-" + entry.getPath() + ".adoc");
					tableWriter.spanColumns(3, 
						entry.getPath() + " - Object", 
						"[bootstrap,collapse, id=\"" + filePrefix + "-" + entry.getPath() + "\" file=\"" + newFile.getAbsolutePath() + "\" " +
						"title=\"Show child attributes\"]\n--\n--");
					writeToSubTable(newFile, rootPath + "." + entry.getPath(), payload, entry.getChildren());
				} else {
					FieldType type = entry.getType() != null ? entry.getType() : FieldSnippetWriter.this.fieldTypeResolver.resolveFieldType(entry.getPath(), payload);
					tableWriter.row(entry.getPath(), type.toString(), entry.getDescription());
				}
			}
		}

		private void writeToSubTable(File outFile, String rootPath, final Map<String, Object> payload, List<FieldDescriptor> descriptors) throws IOException {
			AsciidoctorWriter newWriter = new AsciidoctorWriter(createWriter(outFile));
			newWriter.table(new MyTableAction(rootPath, descriptors, payload));
			newWriter.close();
		}
	}
}
