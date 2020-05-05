package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public class PredicateObjectsListSerializer extends JsonSerializer<PredicateObjectsList> {

	@Override
	public void serialize(PredicateObjectsList value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartArray();

		for (AnnotatedValue<IRI> aPredicate : value.getPredicates()) {
			gen.writeStartObject();
			gen.writeObjectField("predicate", aPredicate);
			gen.writeArrayFieldStart("objects");
			for (AnnotatedValue<?> annotatedObject : Optional
					.ofNullable(value.getValue(aPredicate.getValue())).orElse(Collections.emptyList())) {
				gen.writeObject(annotatedObject);
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}

		gen.writeEndArray();
	}

}
