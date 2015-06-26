package org.springframework.restdocs.util.restassured;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.SAXException;

import com.jayway.restassured.internal.path.json.JsonPrettifier;
import com.jayway.restassured.internal.path.xml.XmlPrettifier;
import groovy.util.XmlParser;

public class DocumentableHttpRequest implements DocumentableRequest {

	private final HttpRequest request;
	private final HttpContext context;
	private final URI uri;

	public DocumentableHttpRequest(HttpRequest request, HttpContext context) throws MalformedURLException, URISyntaxException {
		this.request = request;
		this.context = context;
		HttpHost host = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
		this.uri = new URI(host.toURI()).resolve(request.getRequestLine().getUri());
	}

	@Override
	public boolean isGetRequest() {
		return HttpGet.METHOD_NAME.equals(request.getRequestLine().getMethod()); 
	}

	@Override
	public boolean isPostRequest() {
		return HttpPost.METHOD_NAME.equals(request.getRequestLine().getMethod());
	}
	
	@Override 
	public boolean isDeleteRequest() {
		return HttpDelete.METHOD_NAME.equals(request.getRequestLine().getMethod());
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headersByName = new HashMap<>();
		for (Header header : request.getAllHeaders()) {
			List<String> headerValues = headersByName.get(header.getName());
			if (headerValues == null) {
				headerValues = new ArrayList<>();
			}
			headerValues.add(header.getValue());
			headersByName.put(header.getName(), headerValues);
		}
		return headersByName;
	}

	@Override
	public String getScheme() throws MalformedURLException {
		return uri.getScheme();
	}

	@Override
	public String getHost() {
		return uri.getHost();
	}

	@Override
	public int getPort() {
		return uri.getPort();
	}

	@Override
	public String getMethod() {
		return request.getRequestLine().getMethod();
	}

	@Override
	public long getContentLength() {
		Map<String, List<String>> headers = getHeaders();
		long contentLength = 0;
		if (headers.containsKey("Content-Length")) {
			contentLength = Long.parseLong(headers.get("Content-Length").get(0));
		}
		return contentLength;
	}

	@Override
	public String getContentAsString(boolean prettify) throws IOException {
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.request;
			BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entityEnclosingRequest.getEntity());
			entityEnclosingRequest.setEntity(bufferedHttpEntity);
			if (prettify) {
				return getPrettyContent(bufferedHttpEntity);
			} else {
				return EntityUtils.toString(bufferedHttpEntity);
			}
		}
		return "";	
	}

	private String getPrettyContent(BufferedHttpEntity bufferedHttpEntity) throws IOException {
		String content = EntityUtils.toString(bufferedHttpEntity);
		try {
			ContentType contentType = ContentType.get(bufferedHttpEntity);
			if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType())) {
				return JsonPrettifier.prettifyJson(content);
			} else if (ContentType.APPLICATION_XML.getMimeType().equals(contentType.getMimeType())) {
				return XmlPrettifier.prettify(new XmlParser(false, false), content);
			} else if (ContentType.TEXT_HTML.getMimeType().equals(contentType.getMimeType())) {
				return XmlPrettifier.prettify(new XmlParser(new Parser()), content);
			}
		} catch (ParserConfigurationException | SAXException e) {
			// abandon attempt to prettify
		}
		return content;
	}

	@Override
	public String getRequestUriWithQueryString() {
		return request.getRequestLine().getUri();
	}

	@Override
	public String getParameterMapAsQueryString() {
		return null;
	}
}
