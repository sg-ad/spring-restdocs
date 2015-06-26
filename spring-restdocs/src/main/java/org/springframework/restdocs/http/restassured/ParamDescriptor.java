package org.springframework.restdocs.http.restassured;

public class ParamDescriptor {

	private final String name;

	private boolean optional;

	private String description;

	ParamDescriptor(String name) {
		this.name = name;
	}

	/**
	 * Marks the field as optional
	 *
	 * @return {@code this}
	 */
	public ParamDescriptor optional() {
		this.optional = true;
		return this;
	}

	/**
	 * Specifies the description of the field
	 *
	 * @param description The field's description
	 * @return {@code this}
	 */
	public ParamDescriptor description(String description) {
		this.description = description;
		return this;
	}

	public String getName() {
		return this.name;
	}

	public boolean isOptional() {
		return this.optional;
	}

	public String getDescription() {
		return this.description;
	}
}
