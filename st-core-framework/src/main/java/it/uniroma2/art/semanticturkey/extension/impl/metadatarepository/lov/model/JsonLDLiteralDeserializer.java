package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model;

import java.io.IOException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JsonLDLiteralDeserializer extends StdDeserializer<Literal> {

	private static final long serialVersionUID = -1312633197523147503L;

	public JsonLDLiteralDeserializer() {
		super(Literal.class);
	}

	@Override
	public Literal deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode jsonObject = p.readValueAsTree();
		String label = jsonObject.get("value").asText();
		JsonNode jsonLang = jsonObject.get("lang");
		String lang;
		if (jsonLang != null && !jsonLang.isNull()) {
			lang = jsonLang.asText();
		} else {
			lang = "unk";
		}

		return SimpleValueFactory.getInstance().createLiteral(label, lang);
	}

}
