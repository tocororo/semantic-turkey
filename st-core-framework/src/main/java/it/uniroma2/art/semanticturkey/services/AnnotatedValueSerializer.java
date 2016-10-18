package it.uniroma2.art.semanticturkey.services;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class AnnotatedValueSerializer extends JsonSerializer<AnnotatedValue<?>>{

	@Override
	public void serialize(AnnotatedValue<?> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		Value payloadValue = value.getValue();
		
		gen.writeStartObject();
		
		if (payloadValue instanceof Resource) {
			gen.writeStringField("@id", value.getStringValue());
		} else {
			gen.writeStringField("@value", value.getStringValue());
		}
		
		Map<String, Value> payloadAttributes = value.getAttributes();
		
		for (Map.Entry<String, Value> entry : payloadAttributes.entrySet()) {
			String attrKey = entry.getKey();
			Value attrValue = entry.getValue();
			
			if (attrValue instanceof Literal) {
				Literal attrValueLit = (Literal)attrValue;
				IRI datatype = attrValueLit.getDatatype();
				
				if (datatype.equals(XMLSchema.BOOLEAN)) {
					gen.writeBooleanField(attrKey, attrValueLit.booleanValue());
				} else if (datatype.equals(XMLSchema.INTEGER)) {
					gen.writeNumberField(attrKey, attrValueLit.intValue());
				} else {
					
					Optional<String> languageTag = attrValueLit.getLanguage();
					
					if (languageTag.isPresent()) {
						gen.writeObjectFieldStart(attrKey);
						gen.writeStringField("@value", attrValueLit.stringValue());
						gen.writeStringField("@language", languageTag.get());
						gen.writeEndObject();
					} else {
						if (datatype.equals(XMLSchema.STRING)) {
							gen.writeStringField(attrKey, attrValueLit.stringValue());
						} else {
							gen.writeObjectFieldStart(attrKey);
							gen.writeStringField("@value", attrValueLit.stringValue());
							gen.writeStringField("@type", datatype.stringValue());
							gen.writeEndObject();
						}
					}
					
				}
			} else {
				gen.writeStringField(attrKey, attrValue.stringValue());
			}
		}

		gen.writeEndObject();
	}

}
