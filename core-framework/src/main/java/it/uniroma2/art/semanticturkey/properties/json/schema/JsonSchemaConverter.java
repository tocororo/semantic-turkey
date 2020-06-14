package it.uniroma2.art.semanticturkey.properties.json.schema;

import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.util.iterators.Iterators;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.BooleanSchema;
import org.everit.json.schema.EnumSchema;
import org.everit.json.schema.NumberSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.AnnotatedTypeBuilder;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.PropertyDefinition;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * Converters a JSON schema to a {@link RuntimeSTProperties}. This converter supports the following keywords:
 * <ul>
 * <li>type</li>
 * <li>enum</li>
 * <li>properties</li>
 * <li>required</li>
 * <li>minLength</li>
 * <li>maxLength</li>
 * <li>pattern</li>
 * <li>minimum</li>
 * <li>maximum</li>
 * <li>items</li>
 * <li>uniqueItems</li>
 * <li>title</li>
 * <li>description</li>
 * <li>default</l>
 * </ul>
 * 
 * <p>
 * The underlying JSON Schema parser is updated to draft v7; however, we encourage to only use keywords that
 * didn't have backward incompatible changes into subsequent specs.
 * </p>
 * <p>
 * The underlying library looses the order of property declarations: we fixed this problem by joining with the
 * represented obtained through Jackson. Another limitation of the library is the lack of support for default
 * values, which we implemented separately.
 * </p>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class JsonSchemaConverter {
	private ObjectMapper objectMapper = new ObjectMapper();

	public RuntimeSTProperties convert(Reader reader) throws ConversionException {
		try {
			JsonNode jacksonSchemaNode = objectMapper.readTree(reader);
			return convert(jacksonSchemaNode);
		} catch (IOException e) {
			throw new ConversionException(e);
		}
	}

	public RuntimeSTProperties convert(JsonNode jacksonSchemaNode) throws ConversionException {
		try {
			ObjectMapper stPropertiesObjectMapper = STPropertiesManager.createObjectMapper();

			JSONObject schemaJson = new JSONObject(
					new JSONTokener(objectMapper.writeValueAsString(jacksonSchemaNode)));
			Schema schema = SchemaLoader.load(schemaJson);

			if (!(schema instanceof ObjectSchema)) {
				throw new ConversionException("The supplied schema is not an object schema");
			}

			ObjectSchema objectSchema = (ObjectSchema) schema;

			RuntimeSTProperties stProps = new RuntimeSTProperties(
					Optional.ofNullable(objectSchema.getTitle()).orElse(""));

			String description = objectSchema.getDescription();
			if (description != null) {
				stProps.setHtmlDescription(HtmlUtils.htmlEscape(description));
			}

			Set<String> requiredProps = new HashSet<>(objectSchema.getRequiredProperties());

			JsonNode jacksonPropertiesNode = jacksonSchemaNode.get("properties");
			List<String> sortedFields = Optional.ofNullable(jacksonPropertiesNode)
					.map(n -> Iterators.asList(n.fieldNames())).orElse(Collections.emptyList());

			Map<String, Schema> propertySchemas = Optional.ofNullable(objectSchema.getPropertySchemas())
					.orElse(Collections.emptyMap());

			for (String propName : sortedFields) {
				Schema propSchema = propertySchemas.get(propName);
				if (propSchema == null)
					continue;

				PropertyDefinition stPropDef;

				if (propSchema instanceof EnumSchema) {
					EnumSchema enumSchema = (EnumSchema) propSchema;
					Set<Object> possibleValues = enumSchema.getPossibleValues();

					if (!possibleValues.stream().allMatch(String.class::isInstance)) {
						throw new ConversionException("Enumeration with some non string constant");
					}

					stPropDef = new PropertyDefinition(Optional.ofNullable(propSchema.getTitle()).orElse(""),
							Optional.ofNullable(propSchema.getDescription()).orElse(""),
							requiredProps.contains(propName),
							new AnnotatedTypeBuilder().withType(String.class).build());
					stPropDef.setEnumeration(possibleValues.toArray(new String[possibleValues.size()]));

				} else {
					Pair<AnnotatedType, List<Pair<Class<? extends Annotation>, Map<String, Object>>>> fieldTypeAndAnnotations = convertSchemaToAnnotatedTypeAndFieldConstraints(
							propSchema);
					stPropDef = new PropertyDefinition(Optional.ofNullable(propSchema.getTitle()).orElse(""),
							Optional.ofNullable(propSchema.getDescription()).orElse(""),
							requiredProps.contains(propName), fieldTypeAndAnnotations.getLeft());

					fieldTypeAndAnnotations.getRight()
							.forEach(p -> stPropDef.addAnnotation(p.getLeft(), p.getRight()));
				}

				stProps.addProperty(propName, stPropDef);

				ObjectNode jacksonPropNode = (ObjectNode) jacksonPropertiesNode.get(propName);
				JsonNode defaultJsonValue = jacksonPropNode.get("default");

				if (defaultJsonValue != null && !defaultJsonValue.isNull()) {
					Object defaultValue = stPropertiesObjectMapper
							.readValue(objectMapper.treeAsTokens(defaultJsonValue), objectMapper
									.getTypeFactory().constructType(stPropDef.getAnnotatedType().getType()));
					try {
						stProps.setPropertyValue(propName, defaultValue);
					} catch (WrongPropertiesException e) {
						throw new ConversionException(e);
					}
				}
			}

			return stProps;
		} catch (JSONException | IOException e) {
			throw new ConversionException(e);
		}

	}

	public Pair<AnnotatedType, List<Pair<Class<? extends Annotation>, Map<String, Object>>>> convertSchemaToAnnotatedTypeAndFieldConstraints(
			Schema schema) throws ConversionException {
		AnnotatedType annotatedType;
		List<Pair<Class<? extends Annotation>, Map<String, Object>>> annotations = new ArrayList<>();

		if (schema instanceof BooleanSchema) {
			annotatedType = new AnnotatedTypeBuilder().withType(Boolean.class).build();
		} else if (schema instanceof StringSchema) {
			StringSchema stringSchema = (StringSchema) schema;

			AnnotatedTypeBuilder typeBuilder = new AnnotatedTypeBuilder().withType(String.class);
			annotatedType = typeBuilder.build();

			int min = stringSchema.getMinLength() != null ? stringSchema.getMinLength() : 0;
			int max = stringSchema.getMaxLength() != null ? stringSchema.getMaxLength() : Integer.MAX_VALUE;

			if (min != 0 || max != Integer.MAX_VALUE) {
				annotations.add(ImmutablePair.of(Size.class, ImmutableMap.of("min", min, "max", max)));
			}

			Pattern pattern = stringSchema.getPattern();

			if (pattern != null) {
				annotations.add(ImmutablePair.of(javax.validation.constraints.Pattern.class,
						ImmutableMap.of("regexp", pattern.pattern())));
			}

		} else if (schema instanceof NumberSchema) {
			NumberSchema numberSchema = (NumberSchema) schema;

			Class<?> clazz = numberSchema.requiresInteger() ? Integer.class : Float.class;
			AnnotatedTypeBuilder builder = new AnnotatedTypeBuilder();
			builder.withType(clazz);
			annotatedType = builder.build();

			if (clazz == Integer.class) {
				Number minimum = numberSchema.getMinimum();
				if (minimum instanceof Integer) {
					long min;
					if (numberSchema.isExclusiveMinimum()) {
						min = (int) minimum - 1;
					} else {
						min = (int) minimum;
					}

					annotations.add(ImmutablePair.of(Min.class, ImmutableMap.of("value", min)));
				}

				Number maximum = numberSchema.getMaximum();
				if (maximum instanceof Integer) {
					long max;
					if (numberSchema.isExclusiveMaximum()) {
						max = (int) maximum + 1;
					} else {
						max = (int) maximum;
					}

					annotations.add(ImmutablePair.of(Max.class, ImmutableMap.of("value", max)));
				}

			}
		} else if (schema instanceof ObjectSchema) {
			annotatedType = new AnnotatedTypeBuilder().withType(ObjectNode.class).build();
		} else if (schema instanceof ArraySchema) {
			ArraySchema arraySchema = (ArraySchema) schema;
			Class<?> clazz = arraySchema.needsUniqueItems() ? Set.class : List.class;

			Schema itemsSchema = arraySchema.getAllItemSchema();

			if (itemsSchema == null) {
				throw new ConversionException("Missing items schema on array property");
			}

			Pair<AnnotatedType, List<Pair<Class<? extends Annotation>, Map<String, Object>>>> itemsType = convertSchemaToAnnotatedTypeAndFieldConstraints(
					itemsSchema);

			if (!itemsType.getRight().isEmpty()) {
				throw new ConversionException("Could not apply field annotations to items type");
			}
			annotatedType = new AnnotatedTypeBuilder().withType(clazz).withTypeArgument(itemsType.getLeft())
					.build();
		} else {
			throw new ConversionException("Unsupported schema type: " + schema.getClass().getSimpleName());
		}

		return ImmutablePair.of(annotatedType, annotations);
	}
}
