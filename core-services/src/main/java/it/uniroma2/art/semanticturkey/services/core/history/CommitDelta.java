package it.uniroma2.art.semanticturkey.services.core.history;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.uniroma2.art.semanticturkey.services.core.history.CommitDelta.CommitDeltaSerializer;

/**
 * Delta associated with a commit.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@JsonSerialize(using = CommitDeltaSerializer.class)
public class CommitDelta {
	private Model additions;
	private Model removals;
	private int additionsTruncated;
	private int removalsTruncated;

	public Model getAdditions() {
		return additions;
	}

	public void setAdditions(Model additions) {
		this.additions = additions;
	}

	public Model getRemovals() {
		return removals;
	}

	public void setRemovals(Model removals) {
		this.removals = removals;
	}

	public int getAdditionsTruncated() {
		return additionsTruncated;
	}

	public void setAdditionsTruncated(int additionsTruncated) {
		this.additionsTruncated = additionsTruncated;
	}

	public int getRemovalsTruncated() {
		return removalsTruncated;
	}

	public void setRemovalsTruncated(int removalsTruncated) {
		this.removalsTruncated = removalsTruncated;
	}

	public static class CommitDeltaSerializer extends StdSerializer<CommitDelta> {

		private static final long serialVersionUID = 1L;

		public CommitDeltaSerializer() {
			this(null);
		}

		public CommitDeltaSerializer(Class<CommitDelta> t) {
			super(t);
		}

		@Override
		public void serialize(CommitDelta value, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeStartObject();

			if (value.additionsTruncated != 0) {
				gen.writeNumberField("additionsTruncated", value.additionsTruncated);
			}

			if (value.removalsTruncated != 0) {
				gen.writeNumberField("removalsTruncated", value.removalsTruncated);
			}

			gen.writeArrayFieldStart("additions");
			for (Statement st : value.additions) {
				writeStatement(gen, st);
			}
			gen.writeEndArray();

			gen.writeArrayFieldStart("removals");
			for (Statement st : value.removals) {
				writeStatement(gen, st);
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}

		private void writeStatement(JsonGenerator gen, Statement st) throws IOException {
			gen.writeStartObject();

			gen.writeObjectFieldStart("subject");
			writeValue(gen, st.getSubject());
			gen.writeEndObject();

			gen.writeObjectFieldStart("predicate");
			writeValue(gen, st.getPredicate());
			gen.writeEndObject();

			gen.writeObjectFieldStart("object");
			writeValue(gen, st.getObject());
			gen.writeEndObject();

			gen.writeObjectFieldStart("context");
			writeValue(gen, st.getContext());
			gen.writeEndObject();

			gen.writeEndObject();
		}

		private void writeValue(JsonGenerator gen, Value value) throws IOException {
			if (value instanceof Resource) {
				String idValue;

				if (value instanceof BNode) {
					idValue = "_:" + value.stringValue();
				} else {
					idValue = value.stringValue();
				}
				gen.writeStringField("@id", idValue);
			} else {
				Literal payloadLiteral = (Literal) value;
				gen.writeStringField("@value", payloadLiteral.getLabel());
				Optional<String> langHolder = payloadLiteral.getLanguage();
				if (langHolder.isPresent()) {
					gen.writeStringField("@language", langHolder.get());
				} else {
					gen.writeStringField("@type", payloadLiteral.getDatatype().stringValue());
				}
			}
		}

	}

}
