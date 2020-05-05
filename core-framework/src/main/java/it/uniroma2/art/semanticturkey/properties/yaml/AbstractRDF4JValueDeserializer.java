package it.uniroma2.art.semanticturkey.properties.yaml;

import java.io.IOException;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Converts the NT serialization of an RDF term to an object implementing {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class AbstractRDF4JValueDeserializer<T extends Value> extends StdDeserializer<T> {
	private static final long serialVersionUID = 120048372687202776L;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private Class<T> expectedType;

	public AbstractRDF4JValueDeserializer(Class<T> expectedType) {
		super(expectedType);
		this.expectedType = expectedType;
	}

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		String NTTerm = p.getText();

		if (NTTerm == null) {
			ctxt.reportMappingException("Expected a string token");
		}
		Value value = NTriplesUtil.parseValue(NTTerm, vf);

		Class<? extends Value> actualType = value.getClass();

		if (expectedType.isAssignableFrom(actualType)) {
			return expectedType.cast(value);
		} else {
			ctxt.reportMappingException(new StringBuilder(actualType.getSimpleName()).append(" given, but ")
					.append(expectedType.getSimpleName()).append(" expected").toString());
		}

		throw new IllegalStateException("The program should never arrive here");
	}

}
