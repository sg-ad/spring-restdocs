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

package org.springframework.restdocs.snippet.restassured;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.springframework.restdocs.snippet.AsciidoctorWriter;
import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.OutputFileResolver;
import org.springframework.test.web.servlet.ResultHandler;

/**
 * Base class for a {@link ResultHandler} that writes a documentation snippet
 * 
 */
public abstract class SnippetWriter {

	protected String outputDir;

	protected String fileName;

	protected SnippetWriter(String outputDir, String fileName) {
		this.outputDir = outputDir;
		this.fileName = fileName;
	}

	protected File resolveFile(String outputDir, String fileName) {
		return new OutputFileResolver().resolve(outputDir, fileName);
	}
	
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws Exception {
		Writer writer = createWriter(resolveFile(this.outputDir, this.fileName + ".adoc"));
		try {
			handle(request, response, context, new AsciidoctorWriter(writer));
		} finally {
			writer.close();
		}
	}
	
	public abstract void handle(HttpRequest request, HttpResponse response, HttpContext context, DocumentationWriter writer) throws Exception;

	public static Writer createWriter(File outputFile) throws IOException {
		if (outputFile != null) {
			File parent = outputFile.getParentFile();
			if (!parent.isDirectory() && !parent.mkdirs()) {
				throw new IllegalStateException("Failed to create directory '" + parent + "'");
			}
			return new FileWriter(outputFile);
		}
		else {
			return new OutputStreamWriter(System.out);
		}
	}

}
