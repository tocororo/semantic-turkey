package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Type information used in the definiion of a custom service.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see CustomService
 */
public class Type {

	private String name;
	private List<Type> typeArguments;

	@JsonCreator
	public Type(@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "typeArguments", required = false) List<Type> typeArguments) {
		this.name = name;
		this.typeArguments = typeArguments;
	}

	public String getName() {
		return name;
	}

	public List<Type> getTypeArguments() {
		return typeArguments;
	}
}
