package it.uniroma2.art.semanticturkey.services;

import java.util.Collections;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(using=AnnotatedValueSerializer.class)
public class AnnotatedValue<T extends Value> {

	private final T value;
	private final Map<String, Value> attributes;
	
	public AnnotatedValue(T value) {
		this(value, Collections.emptyMap());
	}

	
	public AnnotatedValue(T value, Map<String, Value> attributes) {
		this.value = value;
		this.attributes = attributes;
	}
	
	public T getValue() {
		return value;
	}
	
	public String getType() {
		if (value instanceof Literal) {
			return "literal";
		} else if (value instanceof BNode) {
			return "bnode";
		} else {
			return "iri";
		}
	}
	
	public String getStringValue() {
		return value.stringValue();
	}

	public String getLanguage() {
		return value instanceof Literal ? ((Literal)value).getLanguage().orElse(null) : null;
	}
	
	public String getDatatype() {
		return value instanceof Literal ? ((Literal)value).getDatatype().stringValue() : null;
	}
	
	public Map<String, Value> getAttributes() {
		return attributes;
	}
}
