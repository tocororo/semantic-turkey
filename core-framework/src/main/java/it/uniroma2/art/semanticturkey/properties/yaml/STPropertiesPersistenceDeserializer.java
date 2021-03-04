package it.uniroma2.art.semanticturkey.properties.yaml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.databind.introspect.VirtualAnnotatedMember;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.Schema;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties;
import it.uniroma2.art.semanticturkey.properties.dynamic.STPropertiesSchema;

/**
 * A Jackson's {@link JsonDeserializer} for reading {@link STProperties} from files. This deserializer does
 * not process metadata that are instead included in the serialization for the UI.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STPropertiesPersistenceDeserializer extends StdDeserializer<STProperties>
		implements ContextualDeserializer {

	private static final long serialVersionUID = -3607180509353754110L;

	private ExtensionPointManager exptManager;
	private Schema schemaAnnotation;

	public STPropertiesPersistenceDeserializer(Class<?> vc, ExtensionPointManager exptManager) {
		super(vc);
		this.exptManager = exptManager;
	}

	public STPropertiesPersistenceDeserializer() {
		super(STProperties.class);
	}

	public STPropertiesPersistenceDeserializer(STPropertiesPersistenceDeserializer base) {
		super(base);
		this.exptManager = base.exptManager;
		this.schemaAnnotation = base.schemaAnnotation;
	}

	@Override
	public STProperties deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		try {
			TreeNode treeNode = p.readValueAsTree();
			if (treeNode instanceof NullNode) {
				return null;
			}
			ObjectNode node = (ObjectNode) treeNode;

			Class<?> target = handledType();

			if (TypeUtils.equals(target, STProperties.class)) {
				target = DynamicSTProperties.class;
			}
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

			STProperties stProp;

			if (schemaAnnotation != null && TypeUtils.equals(target, DynamicSTProperties.class)) {
				@SuppressWarnings("unchecked")
				SystemSettingsManager<STPropertiesSchema> schemaProvider = (SystemSettingsManager<STPropertiesSchema>) exptManager
						.getSettingsManager(schemaAnnotation.settingsManager().getName());
				STPropertiesSchema propertiesSchema = schemaProvider.getSystemSettings();
				stProp = propertiesSchema.toSTProperties();
			} else {
				stProp = (STProperties) target.newInstance();
			}

			Iterator<String> propIt = node.fieldNames();
			while (propIt.hasNext()) {
				String jsonPropName = propIt.next();
				JsonNode jsonPropValue = node.get(jsonPropName);
				if (!stProp.getProperties().contains(jsonPropName))
					continue; // skip unknown prop names
				AnnotatedType targetType = stProp.getPropertyAnnotatedType(jsonPropName);

				// p.getCodec().
				// ctxt.reader valueId, forProperty,
				// beanInstance)readValue(p.getCodec().treeAsTokens(jsonPropValue),
				// ctxt.constructType(targetType.getType()));

				BeanProperty prop = createBeanProperty(ctxt, target, stProp, jsonPropName, targetType);
				JsonParser treeAsTokens = p.getCodec().treeAsTokens(jsonPropValue);
				JsonToken jsonToken = treeAsTokens.nextToken();
				Object targetValue;
				if (jsonToken == JsonToken.VALUE_NULL) {
					targetValue = null;
				} else {
					targetValue = ctxt.readPropertyValue(treeAsTokens, prop,
							ctxt.constructType(targetType.getType()));
				}
				if (targetValue != null) {
					stProp.setPropertyValue(jsonPropName, targetValue);
				}
			}

			return stProp;
		} catch (PropertyNotFoundException | WrongPropertiesException | InstantiationException
				| IllegalAccessException | ClassNotFoundException | STPropertyAccessException
				| NoSuchSettingsManager e) {
			e.printStackTrace();
			ctxt.reportMappingException("unable to deserialize: " + e.getMessage());
		}

		return null;
	}

	protected VirtualBeanPropertyWriter createBeanProperty(DeserializationContext ctxt,
			Class<?> declaringClass, STProperties bean, String name, AnnotatedType annotatedType)
			throws PropertyNotFoundException {
		Schema schemaAnnotationOnProp = (Schema) Arrays.stream(bean.getAnnotations(name))
				.filter(ann -> ann.annotationType().equals(Schema.class)).findAny().orElse(null);

		JavaType javaType = ctxt.constructType(annotatedType.getType());
		return new VirtualSTPropertyWriter(
				SimpleBeanPropertyDefinition.construct(ctxt.getConfig(),
						(VirtualAnnotatedMember) new VirtualSTProperty(null, declaringClass, name, javaType)),
				null, javaType, schemaAnnotationOnProp);
	}

	private static class VirtualSTProperty extends VirtualAnnotatedMember {

		private static final long serialVersionUID = 5622005865359304179L;

		public VirtualSTProperty(TypeResolutionContext typeContext, Class<?> declaringClass, String name,
				JavaType type) {
			super(typeContext, declaringClass, name, type);
		}

		public void setValue(Object pojo, Object value) throws IllegalArgumentException {
			try {
				((STProperties) pojo).setPropertyValue(getName(), value);
			} catch (WrongPropertiesException e) {
				throw new IllegalArgumentException(e);
			}
		};

	}

	private static class VirtualSTPropertyWriter extends VirtualBeanPropertyWriter {

		private static final long serialVersionUID = -164809230673272307L;

		private Schema schemaAnnotationOnProp;

		public VirtualSTPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
				JavaType declaredType, Schema schemaAnnotationOnProp) {
			super(propDef, contextAnnotations, declaredType);
			this.schemaAnnotationOnProp = schemaAnnotationOnProp;
		}

		@Override
		protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
			return null;
		}

		@Override
		public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
				BeanPropertyDefinition propDef, JavaType type) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <A extends Annotation> A getAnnotation(Class<A> acls) {
			if (Schema.class.equals(acls)) {
				return (A) schemaAnnotationOnProp;
			} else {
				return super.getAnnotation(acls);
			}
		}
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
			throws JsonMappingException {
		if (property != null) {
			STPropertiesPersistenceDeserializer other = new STPropertiesPersistenceDeserializer(this);
			other.schemaAnnotation = property.getAnnotation(Schema.class);
			return other;
		}
		return this;
	}

}