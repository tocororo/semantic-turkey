package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsSection.FacetStructure;

public class PropertyFacetsSectionSerializer extends JsonSerializer<PropertyFacetsSection> {

	@Override
	public void serialize(PropertyFacetsSection value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();

		for (Map.Entry<String, PropertyFacetsSection.FacetStructure> facet : value.getFacets().entrySet()) {
			String facetName = facet.getKey();
			FacetStructure facetStructure = facet.getValue();

			gen.writeObjectFieldStart(facetName);
			gen.writeBooleanField("value", facetStructure.hold());
			gen.writeBooleanField("explicit", facetStructure.isExplicit());
			gen.writeStringField("tripleScope", facetStructure.getTripleScope().toString());
			gen.writeEndObject();

		}

		gen.writeObjectField("inverseOf", value.getInverseOf());

		gen.writeEndObject();
	}

}
