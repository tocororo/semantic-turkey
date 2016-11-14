package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.NameTransformer;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

public class AnnotatedResourceWithMembersSerializer extends JsonSerializer<AnnotatedResourceWithMembers<?,?>>{

	private final boolean unwrappingSerializer;
	private final NameTransformer unwrapper;

	public AnnotatedResourceWithMembersSerializer() {
		this(false, NameTransformer.NOP);
	}
	
	public AnnotatedResourceWithMembersSerializer(boolean unwrappingSerializer, NameTransformer unwrapper) {
		this.unwrappingSerializer = unwrappingSerializer;
		this.unwrapper = unwrapper;
	}
	
	@Override
	public boolean isUnwrappingSerializer() {
		return unwrappingSerializer;
	}
	
    public JsonSerializer<AnnotatedResourceWithMembers<?,?>> unwrappingSerializer(NameTransformer unwrapper) {
        return new AnnotatedResourceWithMembersSerializer(true, unwrapper);
    }
    
	
	@Override
	public void serialize(AnnotatedResourceWithMembers<?,?> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		Value payloadValue = value.getValue();
		
		if (!isUnwrappingSerializer()) {
			gen.writeStartObject();
		}
		
		if (payloadValue instanceof Resource) {
			String idValue;
			
			if (payloadValue instanceof BNode) {
				idValue = "_:" + value.getStringValue();
			} else {
				idValue = value.getStringValue();
			}
			gen.writeStringField(unwrapper.transform("@id"), idValue);
		} else {
			gen.writeStringField(unwrapper.transform("@value"), value.getStringValue());
		}
		
		Map<String, Value> payloadAttributes = value.getAttributes();
		
		for (Map.Entry<String, Value> entry : payloadAttributes.entrySet()) {
			String attrKey = entry.getKey();
			Value attrValue = entry.getValue();
			
			if (attrValue instanceof Literal) {
				Literal attrValueLit = (Literal)attrValue;
				IRI datatype = attrValueLit.getDatatype();
				
				if (datatype.equals(XMLSchema.BOOLEAN)) {
					gen.writeBooleanField(unwrapper.transform(attrKey), attrValueLit.booleanValue());
				} else if (datatype.equals(XMLSchema.INTEGER)) {
					gen.writeNumberField(unwrapper.transform(attrKey), attrValueLit.intValue());
				} else {
					
					Optional<String> languageTag = attrValueLit.getLanguage();
					
					if (languageTag.isPresent()) {
						gen.writeObjectFieldStart(unwrapper.transform(attrKey));
						gen.writeStringField(unwrapper.transform("@value"), attrValueLit.stringValue());
						gen.writeStringField(unwrapper.transform("@language"), languageTag.get());
						gen.writeEndObject();
					} else {
						if (datatype.equals(XMLSchema.STRING)) {
							gen.writeStringField(unwrapper.transform(attrKey), attrValueLit.stringValue());
						} else {
							gen.writeObjectFieldStart(unwrapper.transform(attrKey));
							gen.writeStringField(unwrapper.transform("@value"), attrValueLit.stringValue());
							gen.writeStringField(unwrapper.transform("@type"), datatype.stringValue());
							gen.writeEndObject();
						}
					}
					
				}
			} else {
				gen.writeStringField(unwrapper.transform(attrKey), attrValue.stringValue());
			}
		}
		
		gen.writeArrayFieldStart("members");
		for (AnnotatedValue<?> m : value.getMembers()) {
			gen.writeObject(m);
		}
		gen.writeEndArray();
		
		if (!isUnwrappingSerializer()) {
			gen.writeEndObject();
		}
	}

}
