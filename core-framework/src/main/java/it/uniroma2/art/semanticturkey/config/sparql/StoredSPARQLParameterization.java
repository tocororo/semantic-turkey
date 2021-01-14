package it.uniroma2.art.semanticturkey.config.sparql;

import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A parameterization of a stored SPARQL operation (see {@link StoredSPARQLOperation}). A parameterization
 * consists of:
 * <ul>
 * <li>a <i>reference</i> to a stored SPARQL operation</li>
 * <li>a collection of <i>bindings</i> of variables (occurring in the referenced operation) that can express
 * one of:
 * <ul>
 * <li>an <i>assigned value</i> (i.e. ground the variable)</li>
 * <li>a <i>constraint</i> requiring a <i>role</i> or a specific <i>literal type</i></li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class StoredSPARQLParameterization implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization";

		public static final String shortName = keyBase + ".shortName";
		public static final String relativeReference$description = keyBase + ".relativeReference.description";
		public static final String relativeReference$displayName = keyBase + ".relativeReference.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String variableBindings$description = keyBase + ".variableBindings.description";
		public static final String variableBindings$displayName = keyBase + ".variableBindings.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.relativeReference$description + "}", displayName = "{" + MessageKeys.relativeReference$displayName + "}")
	@Required
	public String relativeReference;

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{" + MessageKeys.description$displayName + "}")
	public String description;

	@STProperty(description = "{" + MessageKeys.variableBindings$description + "}", displayName = "{" + MessageKeys.variableBindings$displayName + "}")
	public Map<String, VariableBinding> variableBindings;

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "bindingType")
	@JsonSubTypes({ @JsonSubTypes.Type(value = GroundingVariableBinding.class),
			@JsonSubTypes.Type(value = ConstraintVariableBinding.class) })
	public abstract static class VariableBinding {
		private final String displayName;
		private final String description;

		public VariableBinding(String displayName, String description) {
			this.displayName = displayName;
			this.description = description;
		}

		public String getBindingType() {
			return this.getClass().getAnnotation(JsonTypeName.class).value();
		}

		public @Nullable String getDisplayName() {
			return displayName;
		}

		public @Nullable String getDescription() {
			return description;
		}

	}

	@JsonTypeName("assignment")
	public static class GroundingVariableBinding extends VariableBinding {
		private final Value value;

		public GroundingVariableBinding(
				@JsonProperty(value = "displayName", required = false) String displayName,
				@JsonProperty(value = "description", required = false) String description,
				@JsonProperty(value = "value", required = true) Value value) {
			super(displayName, description);
			this.value = value;
		}

		public Value getValue() {
			return value;
		}

		@Override
		public String getBindingType() {
			return "assignment";
		}

	}

	@JsonTypeName("constraint")
	public static class ConstraintVariableBinding extends VariableBinding {
		private final @Nullable RDFResourceRole resourceRole;
		private final @Nullable IRI datatype;

		public ConstraintVariableBinding(
				@JsonProperty(value = "displayName", required = false) String displayName,
				@JsonProperty(value = "description", required = false) String description,
				@JsonProperty(value = "resourceRole", required = false) RDFResourceRole resourceRole,
				@JsonProperty(value = "datatype", required = false) IRI datatype) {
			super(displayName, description);

			if (!(resourceRole != null ^ datatype != null)) {
				throw new IllegalArgumentException(
						"Exactly one between resourceRole and datatype shall be non-null");
			}

			this.resourceRole = resourceRole;
			this.datatype = datatype;
		}

		public @Nullable RDFResourceRole getResourceRole() {
			return resourceRole;
		}

		public @Nullable IRI getDatatype() {
			return datatype;
		}
	}
}
