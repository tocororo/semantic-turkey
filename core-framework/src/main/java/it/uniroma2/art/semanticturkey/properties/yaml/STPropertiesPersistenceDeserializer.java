package it.uniroma2.art.semanticturkey.properties.yaml;

import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * A Jackson's {@link JsonDeserializer} for reading {@link STProperties} from files. This deserializer does
 * not process metadata that are instead included in the serialization for the UI.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STPropertiesPersistenceDeserializer extends StdDeserializer<STProperties> {

	private ExtensionPointManager exptManager;

	public STPropertiesPersistenceDeserializer(Class<?> vc, ExtensionPointManager exptManager) {
		super(vc);
		this.exptManager = exptManager;
	}

	public STPropertiesPersistenceDeserializer() {
		super(STProperties.class);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public STProperties deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		try {
			ObjectNode node = p.readValueAsTree();

			Class<?> target = handledType();

			JsonNode typeNode = node.get("@type");
			if (typeNode != null && !typeNode.isNull()) {
				String propsType = typeNode.asText();
				Class<?> declaredType;
				try {
					declaredType = handledType().getClassLoader().loadClass(propsType);
				} catch (ClassNotFoundException e) {
					if (exptManager != null) {
						declaredType = exptManager.getConfigurationClassFromName(propsType)
								.orElseThrow(() -> e);
					} else {
						throw e;
					}
				}
				if (!TypeUtils.isAssignable(declaredType, target)) {
					ctxt.reportMappingException("declared type not assignabled to the expected one");
				}

				target = declaredType;
			}

			STProperties stProp = (STProperties) target.newInstance();

			Iterator<String> propIt = node.fieldNames();
			while (propIt.hasNext()) {
				String jsonPropName = propIt.next();
				JsonNode jsonPropValue = node.get(jsonPropName);
				if (!stProp.getProperties().contains(jsonPropName))
					continue; // skip unknown prop names
				AnnotatedType targetType = stProp.getPropertyAnnotatedType(jsonPropName);

				Object targetValue = p.getCodec().readValue(p.getCodec().treeAsTokens(jsonPropValue),
						ctxt.constructType(targetType.getType()));

				stProp.setPropertyValue(jsonPropName, targetValue);
			}

			return stProp;
		} catch (PropertyNotFoundException | WrongPropertiesException | InstantiationException
				| IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			ctxt.reportMappingException("unable to deserialize: " + e.getMessage());
		}

		return null;
	}

}