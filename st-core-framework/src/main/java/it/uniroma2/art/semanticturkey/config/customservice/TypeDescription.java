package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A schema
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TypeDescription {

	private String name;
	private List<TypeDescription> typeArguments;

	@JsonCreator
	public TypeDescription(@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "typeArguments", required = false) List<TypeDescription> typeArguments) {
		this.name = name;
		this.typeArguments = typeArguments;
	}

	public String getName() {
		return name;
	}

	public List<TypeDescription> getTypeArguments() {
		return typeArguments;
	}
}
