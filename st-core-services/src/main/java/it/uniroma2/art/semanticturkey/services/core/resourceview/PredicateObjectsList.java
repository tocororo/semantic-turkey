package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

@JsonSerialize(using=PredicateObjectsListSerializer.class)
public class PredicateObjectsList extends PredicateValueList<Collection<AnnotatedValue<? extends Value>>>{

	public PredicateObjectsList(Map<IRI, AnnotatedValue<IRI>> propMap,
			Multimap<IRI, AnnotatedValue<? extends Value>> valueMap) {
		super(propMap, valueMap.asMap());
	}
	
	public Collection<AnnotatedValue<? extends Value>> getObjects(IRI predicate) {
		return getValue(predicate);
	}

}
