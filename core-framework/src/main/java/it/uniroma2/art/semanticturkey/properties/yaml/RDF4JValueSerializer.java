package it.uniroma2.art.semanticturkey.properties.yaml;

import java.io.IOException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * /** A {@link JsonSerializer} for RDF4J {@link Value}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RDF4JValueSerializer extends StdSerializer<Value> {
	private static final long serialVersionUID = 1432940744647617486L;

	public RDF4JValueSerializer() {
		super(Value.class);
	}

	@Override
	public void serialize(Value value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		/**
		 * The method NTriplesUtil.toNTriplesString(..) applied to a blank node, transforms the blank node id
		 * in a destructive manner (i.e. not undone by the deserializer). Therefore, we merely return _:id
		 */
		if (value instanceof BNode) {
			gen.writeString("_:" + ((BNode) value).getID());
		} else {
			gen.writeString(NTriplesUtil.toNTriplesString(value));
		}
	}

}
