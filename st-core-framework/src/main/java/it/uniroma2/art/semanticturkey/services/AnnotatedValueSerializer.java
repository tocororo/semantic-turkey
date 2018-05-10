package it.uniroma2.art.semanticturkey.services;

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

public class AnnotatedValueSerializer extends JsonSerializer<AnnotatedValue<?>> {

	private final boolean unwrappingSerializer;
	private final NameTransformer unwrapper;

	public AnnotatedValueSerializer() {
		this(false, NameTransformer.NOP);
	}

	public AnnotatedValueSerializer(boolean unwrappingSerializer, NameTransformer unwrapper) {
		this.unwrappingSerializer = unwrappingSerializer;
		this.unwrapper = unwrapper;
	}

	@Override
	public boolean isUnwrappingSerializer() {
		return unwrappingSerializer;
	}

	public JsonSerializer<AnnotatedValue<?>> unwrappingSerializer(NameTransformer unwrapper) {
		return new AnnotatedValueSerializer(true, unwrapper);
	}

	@Override
	public void serialize(AnnotatedValue<?> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		Value payloadValue = value.getValue();

		if (!isUnwrappingSerializer()) {
			gen.writeStartObject();
		}

		if (payloadValue instanceof Resource) {
			String idValue;

			if (payloadValue instanceof BNode) {
				idValue = "_:" + payloadValue.stringValue();
			} else {
				idValue = payloadValue.stringValue();
			}
			gen.writeStringField(unwrapper.transform("@id"), idValue);
		} else {
			Literal payloadLiteral = (Literal) payloadValue;
			gen.writeStringField(unwrapper.transform("@value"), payloadLiteral.getLabel());
			Optional<String> langHolder = payloadLiteral.getLanguage();
			if (langHolder.isPresent()) {
				gen.writeStringField(unwrapper.transform("@language"), langHolder.get());
			} else {
				gen.writeStringField(unwrapper.transform("@type"),
						payloadLiteral.getDatatype().stringValue());
			}
		}

		Map<String, Value> payloadAttributes = value.getAttributes();

		for (Map.Entry<String, Value> entry : payloadAttributes.entrySet()) {
			String attrKey = entry.getKey();
			Value attrValue = entry.getValue();

			if (attrValue instanceof Literal) {
				Literal attrValueLit = (Literal) attrValue;
				IRI datatype = attrValueLit.getDatatype();

				if (datatype.equals(XMLSchema.BOOLEAN)) {
					gen.writeBooleanField(unwrapper.transform(attrKey), attrValueLit.booleanValue());
				} else if (datatype.equals(XMLSchema.INTEGER)) {
					gen.writeNumberField(unwrapper.transform(attrKey), attrValueLit.intValue());
				} else if (datatype.equals(XMLSchema.LONG)) {
					gen.writeNumberField(unwrapper.transform(attrKey), attrValueLit.longValue());
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

		if (!isUnwrappingSerializer()) {
			gen.writeEndObject();
		}
	}

}
