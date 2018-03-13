package it.uniroma2.art.semanticturkey.properties.yaml;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * A Jackson's {@link JsonSerializer} for the persistence of {@link STProperties} as files. This serializer
 * does not include metadata that are instead included in the serialization for the UI.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STPropertiesPersistenceSerializer extends StdSerializer<STProperties> {

	private static final long serialVersionUID = 1L;

	public STPropertiesPersistenceSerializer() {
		this(STProperties.class);
	}

	public STPropertiesPersistenceSerializer(Class<STProperties> t) {
		super(t);
	}

	@Override
	public void serialize(STProperties value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		try {
			gen.writeStartObject();

//			gen.writeStringField("@type", value.getClass().getName());

			Collection<String> props = value.getProperties();

			for (String prop : props) {
				Object v = value.getPropertyValue(prop);

				if (v != null) {
					gen.writeObjectField(prop, v);
				}
			}

			gen.writeEndObject();
		} catch (PropertyNotFoundException e) {
			throw new IOException(e);
		}
	}

}