package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PredicateValueListSectionSerializer extends JsonSerializer<PredicateValueListSection<?>>{

	@Override
	public void serialize(PredicateValueListSection<?> value, JsonGenerator gen,
			SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeObject(value.getPredicateValueList());
	}

}
