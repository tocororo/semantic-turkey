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

	@Override
	public String getShortName() {
		return "Stored SPARQL Parameterization";
	}

	@STProperty(description = "A relative reference to a stored SPARQL operation", displayName = "Relative reference")
	@Required
	public String relativeReference;

	@STProperty(description = "Variable bindings", displayName = "Variable bindings")
	public Map<String, VariableBinding> variableBindings;

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "bindingType")
	@JsonSubTypes({ @JsonSubTypes.Type(value = GroundingVariableBinding.class),
			@JsonSubTypes.Type(value = ConstraintVariableBinding.class) })
	public abstract static class VariableBinding {
		public String getBindingType() {
			return this.getClass().getAnnotation(JsonTypeName.class).value();
		}

	}

	@JsonTypeName("assignment")
	public static class GroundingVariableBinding extends VariableBinding {
		private final Value value;

		public GroundingVariableBinding(@JsonProperty(value = "value", required = true) Value value) {
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
				@JsonProperty(value = "resourceRole", required = false) RDFResourceRole resourceRole,
				@JsonProperty(value = "datatype", required = false) IRI datatype) {
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
