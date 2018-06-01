package it.uniroma2.art.semanticturkey.services.core.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A filtering pipeline consisting in a sequence of {@link TransformationStep}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@JsonDeserialize(using = TransformationPipeline.TransformationPipelineDeserializer.class)
public class TransformationPipeline {
	private TransformationStep[] steps;

	public TransformationPipeline(TransformationStep[] steps) {
		this.steps = steps;
	}

	public TransformationStep[] getSteps() {
		return steps;
	}

	public static class TransformationPipelineDeserializer extends JsonDeserializer<TransformationPipeline> {

		@Override
		public TransformationPipeline deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			List<TransformationStep> steps = new ArrayList<>();
			JsonToken tok = p.getCurrentToken();
			if (tok != JsonToken.START_ARRAY) {
				throw new JsonParseException(p, "Expected start of array");
			}
			while ((tok = p.nextToken()) != null) {
				if (tok == JsonToken.END_ARRAY)
					break;

				TransformationStep aStep = p.readValueAs(TransformationStep.class);
				steps.add(aStep);
			}

			if (tok != JsonToken.END_ARRAY) {
				throw new JsonParseException(p, "Expected end of array");
			}

			return new TransformationPipeline(steps.toArray(new TransformationStep[steps.size()]));
		}

	}

	public boolean isEmpty() {
		return steps.length == 0;
	}
}
