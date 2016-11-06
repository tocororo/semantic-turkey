package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

@JsonSerialize(using=PredicateObjectsListSerializer.class)
public class PredicateObjectsList {

	private Multimap<IRI, AnnotatedValue<?>> valueMultiMap;
	private Map<IRI, AnnotatedValue<IRI>> propMap;

	public PredicateObjectsList(Map<IRI, AnnotatedValue<IRI>> propMap,
			Multimap<IRI, AnnotatedValue<?>> valueMultiMap) {
		this.propMap = propMap;
		this.valueMultiMap = valueMultiMap;
	}

	@JsonIgnore
	public Collection<AnnotatedValue<IRI>> getPredicates() {
		return propMap.values();
	}

	@JsonIgnore
	public Collection<AnnotatedValue<?>> getValues(IRI predicate) {
		return valueMultiMap.get(predicate);
	}
	
}
