package it.uniroma2.art.semanticturkey.properties;

import java.awt.List;
import java.lang.reflect.AnnotatedType;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.AnnotatedTypeBuilder;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.PropertyDefinition;

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
	public Map<String, STPropertySchema> properties;

	/**
	 * Returns an {@link STProperties} object that conforms to this schema
	 * 
	 * @return
	 */
	public STProperties toSTProperties() {
		RuntimeSTProperties rv = new RuntimeSTProperties();

		if (properties != null) {
			for (Entry<String, STPropertySchema> propDef : properties.entrySet()) {
				String propName = propDef.getKey();
				STPropertySchema propSchema = propDef.getValue();

				boolean required = propSchema.required;
				Map<String, String> description = propSchema.description;
				Map<String, String> displayName = propSchema.displayName;
				String typeName = propSchema.type;
				String containerName = propSchema.container;

				Class<?> container = makeContainerClass(containerName);
				AnnotatedType elementType = new AnnotatedTypeBuilder().withType(makeElementType(typeName))
						.build();

				AnnotatedType annotatedType;

				if (container != null) {
					annotatedType = new AnnotatedTypeBuilder().withType(container)
							.withTypeArgument(elementType).build();
				} else {
					annotatedType = elementType;
				}
				rv.addProperty(propName,
						new PropertyDefinition("a displayName", "a descr", required, annotatedType));
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
