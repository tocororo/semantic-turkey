package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public class PredicateValueListSerializer extends JsonSerializer<PredicateValueList<?>> {

	@Override
	public void serialize(PredicateValueList<?> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartArray();

		for (AnnotatedValue<IRI> aPredicate : value.getPredicates()) {
			gen.writeStartObject();
			gen.writeObjectField("predicate", aPredicate);
			
			Object predicateValue = value.getValue(aPredicate.getValue());
			if (predicateValue instanceof Iterable) {
				gen.writeArrayFieldStart("value");
				
				for (Object obj : (Iterable<?>)predicateValue) {
					gen.writeObject(obj);
				}
				
				gen.writeEndArray();
			} else {
				gen.writeObjectField("value", predicateValue);
			}
			gen.writeEndObject();
		}
		
		gen.writeEndArray();
	}

}
