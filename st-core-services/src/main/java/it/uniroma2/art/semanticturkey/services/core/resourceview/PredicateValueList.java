package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

@JsonSerialize(using=PredicateValueListSerializer.class)
public class PredicateValueList<T> {

	private Map<IRI, AnnotatedValue<IRI>> propMap;
	private Map<IRI, T> valueMap;

	public PredicateValueList(Map<IRI, AnnotatedValue<IRI>> propMap,
			Map<IRI, T> valueMap) {
		this.propMap = propMap;
		this.valueMap = valueMap;
	}

	@JsonIgnore
	public Collection<AnnotatedValue<IRI>> getPredicates() {
		return propMap.values();
	}

	@JsonIgnore
	public T getValue(IRI predicate) {
		return valueMap.get(predicate);
	}
	
}
