package org.springframework.restdocs.util.restassured;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

public interface DocumentableRequest {
	boolean isGetRequest();

	boolean isPostRequest();
	
	boolean isDeleteRequest();

	Map<String, List<String>> getHeaders();

	String getScheme() throws MalformedURLException;

	String getHost();

	int getPort();

	String getMethod();

	long getContentLength();

	String getContentAsString(boolean prettify) throws IOException;

	String getRequestUriWithQueryString();

	String getParameterMapAsQueryString();
}
