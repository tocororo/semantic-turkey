package it.uniroma2.art.semanticturkey.json;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * A {@link StdSerializer} that serializes commons-lang3 {@link Pair}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class PairSerializer extends StdSerializer<Pair<?,?>> {

	private static final long serialVersionUID = -1068877596540715050L;

	public PairSerializer() {
		super(Pair.class, false);
	}

	@Override
	public void serialize(Pair<?,?> value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		gen.writeStartArray();
		gen.writeObject(value.getLeft());
		gen.writeObject(value.getRight());
		gen.writeEndArray();
	}

}
