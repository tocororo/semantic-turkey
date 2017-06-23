package it.uniroma2.art.semanticturkey.properties;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * A Jackson's {@link JsonSerializer} for {@link STProperties}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STPropertiesSerializer extends StdSerializer<STProperties> {

	private static final long serialVersionUID = 1L;

	public STPropertiesSerializer() {
		this(null);
	}

	public STPropertiesSerializer(Class<STProperties> t) {
		super(t);
	}

	@Override
	public void serialize(STProperties value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		try {
			gen.writeStartObject();

			gen.writeStringField("@type", value.getClass().getName());
			gen.writeStringField("shortName", value.getShortName());
			gen.writeBooleanField("editRequired", value.hasRequiredProperties());
			gen.writeArrayFieldStart("properties");

			Collection<String> props = value.getProperties();

			for (String prop : props) {

				gen.writeStartObject();

				String parDescr = value.getPropertyDescription(prop);

				gen.writeStringField("name", prop);
				gen.writeStringField("description", parDescr);
				gen.writeBooleanField("required", value.isRequiredProperty(prop));
				String contentType = value.getPropertyContentType(prop);
				if (contentType != null) {
					gen.writeStringField("type", contentType);
				}

				Optional<Collection<String>> enumerationHolder = value.getEnumeration(prop);

				if (enumerationHolder.isPresent()) {
					gen.writeArrayFieldStart("enumeration");
					Collection<String> enumeration = enumerationHolder.get();

					for (String val : enumeration) {
						gen.writeString(val);
					}
					
					gen.writeEndArray();
				}
				Object parValue = value.getPropertyValue(prop);
				if (parValue != null) {
					gen.writeStringField("value", parValue.toString());
				}

				gen.writeEndObject();
			}

			gen.writeEndArray();

			gen.writeEndObject();
		} catch (PropertyNotFoundException e) {
			throw new IOException(e);
		}
	}

}