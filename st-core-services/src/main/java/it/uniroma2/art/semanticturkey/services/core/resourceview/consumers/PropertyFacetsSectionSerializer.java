package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PropertyFacetsSectionSerializer extends JsonSerializer<PropertyFacetsSection>{

	@Override
	public void serialize(PropertyFacetsSection value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();
		
		if (value.isSymmetric()) {
			gen.writeObjectFieldStart("symmetric");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isSymmetricExplicit());
			gen.writeEndObject();
		}
		
		if (value.isAsymmetric()) {
			gen.writeObjectFieldStart("asymmetric");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isAsymmetricExplicit());
			gen.writeEndObject();
		}
		
		if (value.isFunctional()) {
			gen.writeObjectFieldStart("functional");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isFunctionalExplicit());
			gen.writeEndObject();
		}
		
		if (value.isInverseFunctional()) {
			gen.writeObjectFieldStart("inverseFunctional");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isInverseFunctionalExplicit());
			gen.writeEndObject();
		}
		
		if (value.isReflexive()) {
			gen.writeObjectFieldStart("reflexive");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isReflexiveExplicit());
			gen.writeEndObject();
		}
		
		if (value.isIrreflexive()) {
			gen.writeObjectFieldStart("irreflexive");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isIrreflexiveExplicit());
			gen.writeEndObject();
		}

		if (value.isTransitive()) {
			gen.writeObjectFieldStart("transitive");
			gen.writeBooleanField("value", true);
			gen.writeBooleanField("explicit", value.isTransitiveExplicit());
			gen.writeEndObject();
		}
		
		gen.writeObjectField("inverseOf", value.getInverseOf());
		
		gen.writeEndObject();
	}

}
