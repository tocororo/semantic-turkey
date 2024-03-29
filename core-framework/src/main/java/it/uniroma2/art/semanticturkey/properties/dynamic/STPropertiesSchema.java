package it.uniroma2.art.semanticturkey.properties.dynamic;

import java.lang.reflect.AnnotatedType;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.semanticturkey.properties.Schema;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties.AnnotatedTypeBuilder;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties.PropertyDefinition;

/**
 * A schema for an {@link STProperties} object. It should be used together with the annotation {@link Schema}
 * to implement dynamically typed properties fields.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertiesSchema implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.STPropertiesSchema";

		public static final String shortName = keyBase + ".shortName";
		public static final String properties$description = keyBase + ".properties.description";
		public static final String properties$displayName = keyBase + ".properties.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.properties$description + "}", displayName = "{"
			+ MessageKeys.properties$displayName + "}")
	public List<STPropertySchema> properties;

	/**
	 * Returns an {@link STProperties} object that conforms to this schema
	 * 
	 * @return
	 */
	public STProperties toSTProperties() {
		DynamicSTProperties rv = new DynamicSTProperties();

		if (properties != null) {
			for (STPropertySchema propSchema : properties) {
				boolean required = propSchema.required;
				java.util.List<@LanguageTaggedString Literal> description = propSchema.description;
				java.util.List<@LanguageTaggedString Literal> displayName = propSchema.displayName;
				String typeName = propSchema.type.name;

				AnnotatedType annotatedType = new AnnotatedTypeBuilder().withType(makeElementType(typeName))
						.build();

				PropertyDefinition dynPropDef = new PropertyDefinition(displayName, description, required,
						annotatedType);
				if (propSchema.enumeration != null) {
					dynPropDef.setEnumeration(propSchema.enumeration.values, propSchema.enumeration.open);
				}

				rv.addProperty(propSchema.name, dynPropDef);
			}
		}

		return rv;
	}

	protected Class<?> makeContainerClass(String container) {
		switch (container) {
		case "List":
			return List.class;
		case "Set":
			return Set.class;
		default:
			return null;
		}
	}

	protected Class<?> makeElementType(String typeName) {
		switch (typeName) {
		case "boolean":
			return Boolean.class;
		case "integer":
			return Integer.class;
		case "short":
			return Short.class;
		case "long":
			return Long.class;
		case "float":
			return Float.class;
		case "double":
			return Double.class;
		case "IRI":
			return IRI.class;
		case "BNode":
			return BNode.class;
		case "Resource":
			return Resource.class;
		case "Literal":
			return Literal.class;
		case "RDFValue":
			return Value.class;
		case "URL":
			return URL.class;
		case "java.lang.String":
			return String.class;
		}

		throw new IllegalArgumentException("Unsupported type " + typeName);

	}
}
