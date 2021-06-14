package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

class LocalizedStringDeserializer extends StdDeserializer<Map<String, String>> {

    public LocalizedStringDeserializer() {
        super((Class<?>)null);
    }

    @Override
    public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Map<String, String> result = new TreeMap<>();

        JsonToken tok = p.currentToken();
        if (tok.isStructStart()) { // multiple localizations as a map
            Map<String, String> v = p.readValueAs(new TypeReference<Map<String, String>>() {
            });
            result.putAll(v);
        } else { // single localization as a string
            result.put("", p.getText());
        }

        return result;
    }
}
