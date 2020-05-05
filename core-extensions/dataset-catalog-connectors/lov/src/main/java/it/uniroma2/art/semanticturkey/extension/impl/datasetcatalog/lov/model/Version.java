package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {
	private String name;
	private URL fileURL;
	@JsonDeserialize(contentUsing = LanguageIdDeserializer.class)
	private List<String> languageIds;

	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public URL getFileURL() {
		return fileURL;
	}



	public void setFileURL(URL fileURL) {
		this.fileURL = fileURL;
	}



	public List<String> getLanguageIds() {
		return languageIds;
	}



	public void setLanguageIds(List<String> languageIds) {
		this.languageIds = languageIds;
	}



	public static class LanguageIdDeserializer extends StdDeserializer<String> {

		private static final long serialVersionUID = 7438190934447578050L;

		public LanguageIdDeserializer() {
			super(String.class);
		}

		@Override
		public String deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectNode objectNode = p.readValueAsTree();
			return objectNode.get("label").textValue();
		}

	}
}
