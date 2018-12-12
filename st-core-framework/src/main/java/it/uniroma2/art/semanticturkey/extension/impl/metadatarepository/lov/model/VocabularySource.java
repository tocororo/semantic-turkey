package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model.VocabularySource.VocabularySearchResultDeserializer;

@JsonDeserialize(using = VocabularySearchResultDeserializer.class)
public class VocabularySource {

	private String type;
	private String uri;
	private String prefix;
	private Map<String, String> titles;
	private Map<String, String> descriptions;
	private List<String> tags;
	private List<String> langs;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Map<String, String> getTitles() {
		return titles;
	}

	public void setTitles(Map<String, String> titles) {
		this.titles = titles;
	}

	public Map<String, String> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Map<String, String> descriptions) {
		this.descriptions = descriptions;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getLangs() {
		return langs;
	}

	public void setLangs(List<String> langs) {
		this.langs = langs;
	}

	public static class VocabularySearchResultDeserializer extends StdDeserializer<VocabularySource> {
		private static final long serialVersionUID = 2392282760395151982L;

		protected VocabularySearchResultDeserializer() {
			super(VocabularySource.class);
		}

		@Override
		public VocabularySource deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			VocabularySource result = new VocabularySource();

			ObjectNode tree = p.readValueAsTree();

			result.setType(tree.get("type").asText());
			result.setUri(tree.get("uri").asText());
			result.setPrefix(tree.get("prefix").asText());
			if (tree.has("tags")) {
				result.setTags(StreamSupport.stream(((ArrayNode) tree.get("tags")).spliterator(), false)
						.map(JsonNode::textValue).collect(Collectors.toList()));
			} else {
				result.setTags(Collections.emptyList());
			}

			if (tree.has("langs")) {
				result.setLangs(StreamSupport.stream(((ArrayNode) tree.get("langs")).spliterator(), false)
						.map(JsonNode::textValue).collect(Collectors.toList()));
			} else {
				result.setLangs(Collections.emptyList());
			}

			Iterator<Entry<String, JsonNode>> it = tree.fields();

			Map<String, String> titles = new LinkedHashMap<>();
			Map<String, String> descriptions = new LinkedHashMap<>();

			while (it.hasNext()) {
				Entry<String, JsonNode> fieldEntry = it.next();

				String fieldName = fieldEntry.getKey();
				JsonNode fieldValue = fieldEntry.getValue();

				Map<String, String> map;

				if (fieldName.startsWith("http://purl.org/dc/terms/title")) {
					map = titles;
				} else if (fieldName.startsWith("http://purl.org/dc/terms/description")) {
					map = descriptions;
				} else {
					continue;
				}

				String lang;
				if (fieldName.contains("@")) {
					lang = fieldName.substring(fieldName.indexOf("@") + 1);
				} else {
					lang = "unk";
				}

				map.put(lang, fieldValue.asText());
			}

			result.setTitles(titles);
			result.setDescriptions(descriptions);

			return result;
		}

	}
}
