package org.springframework.restdocs.http.restassured;

public class RequestDocumentation {
	
	public static ParamDescriptor pathParam(String name) {
		return new ParamDescriptor(name);
	}
}
