package it.uniroma2.art.semanticturkey.services.core.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;

/**
 * A filtering step consists of a {@link PluginSpecification} and a collection of graphs it applies to.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class FilteringStep {
	private it.uniroma2.art.semanticturkey.plugin.PluginSpecification filter;
	private IRI[] graphs;

	public FilteringStep(@JsonProperty("filter") PluginSpecification filter,
			@JsonProperty("graphs") @JsonDeserialize(using = FilteringStep.GraphsDeserializer.class) IRI[] graphs) {
		this.filter = filter;
		this.graphs = graphs;
	}

	public PluginSpecification getFilter() {
		return filter;
	}

	public @Nullable IRI[] getGraphs() {
		return graphs;
	}

	public static class GraphsDeserializer extends JsonDeserializer<IRI[]> {

		private static ValueFactory vf = SimpleValueFactory.getInstance();

		@Override
		public IRI[] deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonToken tok = p.getCurrentToken();

			if (tok != JsonToken.START_ARRAY) {
				throw new JsonParseException(p, "Expected start array");
			}

			List<IRI> iris = new ArrayList<>();

			while ((tok = p.nextToken()) != null) {
				if (tok == JsonToken.END_ARRAY)
					break;

				if (tok != JsonToken.VALUE_STRING) {
					throw new JsonParseException(p, "Expected value string");
				}

				iris.add(vf.createIRI(p.getText()));
			}
			if (tok != JsonToken.END_ARRAY) {
				throw new JsonParseException(p, "Expected end array");
			}

			return iris.toArray(new IRI[iris.size()]);
		}

	}
}
