package org.springframework.restdocs.restassured;

import static org.springframework.restdocs.curl.restassured.CurlDocumentation.documentCurlRequest;
import static org.springframework.restdocs.http.restassured.HttpDocumentation.documentHttpRequest;
import static org.springframework.restdocs.http.restassured.HttpDocumentation.documentHttpResponse;
import static org.springframework.restdocs.hypermedia.restassured.RestAssuredHypermediaDocumentation.documentLinks;
import static org.springframework.restdocs.payload.PayloadDocumentation.documentRestAssuredRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.documentRestAssuredRequestPathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.documentRestAssuredRequestQueryParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.documentRestAssuredResponseFields;

import org.springframework.restdocs.http.restassured.ParamDescriptor;
import org.springframework.restdocs.hypermedia.HypermediaDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.hypermedia.LinkExtractor;
import org.springframework.restdocs.hypermedia.LinkExtractors;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;

import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;

public class RestAssuredDocumentFilter implements Filter {

	private final String outputDir;

	RestAssuredDocumentFilter(String outputDir, String contentType) {
		this.outputDir = outputDir;
		DocumentingContextProvider.getContext().getRequestHandlers().add(documentCurlRequest(this.outputDir, contentType));
		DocumentingContextProvider.getContext().getRequestHandlers().add(documentHttpRequest(this.outputDir, contentType));
		DocumentingContextProvider.getContext().getResponseHandlers().add(documentHttpResponse(this.outputDir, contentType));
	}

	/**
	 * Document the links in the response using the given {@code descriptors}. The links
	 * are extracted from the response based on its content type.
	 * <p>
	 * If a link is present in the response but is not described by one of the descriptors
	 * a failure will occur when this handler is invoked. Similarly, if a link is
	 * described but is not present in the response a failure will also occur when this
	 * handler is invoked.
	 *
	 * @param descriptors the link descriptors
	 * @return {@code this}
	 * @see HypermediaDocumentation#linkWithRel(String)
	 * @see LinkExtractors#extractorForContentType(String)
	 */
	public RestAssuredDocumentFilter withLinks(LinkDescriptor... descriptors) {
		return withLinks(null, descriptors);
	}

	/**
	 * Document the links in the response using the given {@code descriptors}. The links
	 * are extracted from the response using the given {@code linkExtractor}.
	 * <p>
	 * If a link is present in the response but is not described by one of the descriptors
	 * a failure will occur when this handler is invoked. Similarly, if a link is
	 * described but is not present in the response a failure will also occur when this
	 * handler is invoked.
	 *
	 * @param linkExtractor used to extract the links from the response
	 * @param descriptors the link descriptors
	 * @return {@code this}
	 * @see HypermediaDocumentation#linkWithRel(String)
	 */
	public RestAssuredDocumentFilter withLinks(LinkExtractor linkExtractor, LinkDescriptor... descriptors) {
		DocumentingContextProvider.getContext().getResponseHandlers().add(documentLinks(this.outputDir, linkExtractor, descriptors));
		return this;
	}

	/**
	 * Document the fields in the request using the given {@code descriptors}.
	 * <p>
	 * If a field is present in the request but is not described by one of the descriptors
	 * a failure will occur when this handler is invoked. Similarly, if a field is
	 * described but is not present in the request a failure will also occur when this
	 * handler is invoked.
	 *
	 * @param descriptors the link descriptors
	 * @return {@code this}
	 * @see PayloadDocumentation#fieldWithPath(String)
	 */
	public RestAssuredDocumentFilter withRequestFields(FieldDescriptor... descriptors) {
		DocumentingContextProvider.getContext().getRequestHandlers().add(documentRestAssuredRequestFields(this.outputDir, descriptors));
		return this;
	}
	
	public RestAssuredDocumentFilter withRequestPathParams(ParamDescriptor... descriptors) {
		DocumentingContextProvider.getContext().getRequestHandlers().add(documentRestAssuredRequestPathParameters(this.outputDir, descriptors));
		return this;
	}
	
	public RestAssuredDocumentFilter withRequestQueryParams(ParamDescriptor... descriptors) {
		DocumentingContextProvider.getContext().getRequestHandlers().add(documentRestAssuredRequestQueryParameters(this.outputDir, descriptors));
		return this;
	}

	/**
	 * Document the fields in the response using the given {@code descriptors}.
	 * <p>
	 * If a field is present in the response but is not described by one of the
	 * descriptors a failure will occur when this handler is invoked. Similarly, if a
	 * field is described but is not present in the response a failure will also occur
	 * when this handler is invoked.
	 *
	 * @param descriptors the link descriptors
	 * @return {@code this}
	 * @see PayloadDocumentation#fieldWithPath(String)
	 */
	public RestAssuredDocumentFilter withResponseFields(FieldDescriptor... descriptors) {
		DocumentingContextProvider.getContext().getResponseHandlers().add(documentRestAssuredResponseFields(this.outputDir, descriptors));
		return this;
	}

	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		return ctx.next(requestSpec, responseSpec);
	}
}
