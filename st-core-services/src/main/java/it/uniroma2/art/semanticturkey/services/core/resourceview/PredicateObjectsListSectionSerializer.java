package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PredicateObjectsListSectionSerializer extends JsonSerializer<PredicateObjectsListSection>{

	@Override
	public void serialize(PredicateObjectsListSection value, JsonGenerator gen,
			SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeObject(value.getPredicateObjectsList());
	}

}
