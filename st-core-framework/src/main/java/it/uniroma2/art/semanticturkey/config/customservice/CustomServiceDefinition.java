package it.uniroma2.art.semanticturkey.config.customservice;

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
 * A <em>custom service</em> definition supports the implementation and deployment of an ST service without
 * writing ordinary Java code.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomServiceDefinition implements Configuration {

	@Override
	public String getShortName() {
		return "Custom Service Definition";
	}

	@STProperty(description = "The name used to group all operations defined by this service", displayName = "Service name")
	@Required
	public String name;

	@STProperty(description = "A description of this custom service", displayName = "Description")
	public String description;

	@STProperty(description = "Definitions of the operations defined by this custom service", displayName = "Operations")
	public Map<String, OperationDefintion> operations;
	
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "bindingType")
	@JsonSubTypes({ @JsonSubTypes.Type(value = GroundingVariableBinding.class),
			@JsonSubTypes.Type(value = ConstraintVariableBinding.class) })
	public abstract static class ServiceOperationDefintion2 {
		private final String displayName;
		private final String description;

		public ServiceOperationDefintion2(String displayName, String description) {
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
	public static class GroundingVariableBinding extends ServiceOperationDefintion2 {
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
	public static class ConstraintVariableBinding extends ServiceOperationDefintion2 {
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
