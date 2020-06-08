package it.uniroma2.art.semanticturkey.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * A {@link StdSerializer} that serializes RDF4J's {@link TupleQueryResult}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TupleQueryResultSerializer extends StdSerializer<TupleQueryResult> {

	private static final long serialVersionUID = -1068877596540715050L;

	public TupleQueryResultSerializer() {
		super(TupleQueryResult.class, false);
	}

	@Override
	public void serialize(TupleQueryResult value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		QueryResultIO.writeTuple(value, TupleQueryResultFormat.JSON, os);

		ObjectCodec codec = gen.getCodec();
		TreeNode resultTree = codec.readTree(codec.getFactory().createParser(os.toByteArray()));
		gen.writeTree(resultTree);
	}

}
