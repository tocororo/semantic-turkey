package it.uniroma2.art.semanticturkey.properties;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.validation.Constraint;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * A Jackson's {@link JsonSerializer} for {@link STProperties}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class STPropertiesSerializer extends StdSerializer<STProperties> {

	private static final long serialVersionUID = 1L;

	public STPropertiesSerializer() {
		this(null);
	}

	public STPropertiesSerializer(Class<STProperties> t) {
		super(t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object,
	 * com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(STProperties value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		try {
			ObjectMapper propertiesObjectMapper = STPropertiesManager.createObjectMapper();

			gen.writeStartObject();

			gen.writeStringField("@type", value.getClass().getName());
			gen.writeStringField("shortName", value.getShortName());
			gen.writeBooleanField("editRequired", value.hasRequiredProperties());

			if (value.getHTMLDescription() != null) {
				gen.writeStringField("htmlDescription", value.getHTMLDescription());
			}

			if (value.getHTMLWarning() != null) {
				gen.writeStringField("htmlWarning", value.getHTMLWarning());
			}

			gen.writeArrayFieldStart("properties");

			Collection<String> props = value.getProperties();

			for (String prop : props) {

				gen.writeStartObject();

				String parDescr = value.getPropertyDescription(prop);
				AnnotatedType parType = value.getPropertyAnnotatedType(prop);
				String parDispName = value.getPropertyDisplayName(prop);

				gen.writeStringField("name", prop);
				gen.writeStringField("description", parDescr);
				gen.writeStringField("displayName", parDispName);
				gen.writeBooleanField("required", value.isRequiredProperty(prop));
				String contentType = value.getPropertyContentType(prop);
				if (contentType != null) {
					gen.writeObjectFieldStart("type");
					gen.writeStringField("name", contentType);
					gen.writeEndObject();
				} else {
					gen.writeFieldName("type");
					writeTypeDescription(parType, gen, provider);
				}

				Optional<Collection<String>> enumerationHolder = value.getEnumeration(prop);

				if (enumerationHolder.isPresent()) {
					gen.writeArrayFieldStart("enumeration");
					Collection<String> enumeration = enumerationHolder.get();

					for (String val : enumeration) {
						gen.writeString(val);
					}

					gen.writeEndArray();
				}
				
				List<Annotation> constraints = selectConstraints(value.getAnnotations(prop));
				appendConstraints(constraints, gen, provider);
				
				Object parValue = value.getPropertyValue(prop);
				if (parValue != null) {
					// Serializes property values using the ObjectMapper specific for STProperties
					ObjectCodec oldCodec = gen.getCodec();
					try {
						gen.setCodec(propertiesObjectMapper);
						gen.writeObjectField("value", parValue);
					} finally {
						gen.setCodec(oldCodec);
					}
				}

				gen.writeEndObject();
			}

			gen.writeEndArray();

			gen.writeEndObject();
		} catch (PropertyNotFoundException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes down a type. If a type is neither an array type {@link TypeUtils#isArrayType(Type)} nor a
	 * parameterized type {@link ParameterizedType}, then the type is simply serialized as a string
	 * corresponding to its name after applying some reductions (see {@link #computeReducedTypeName(Type)}).
	 * Otherwise, a JSON object is introduced, which have the field {@code name} holding the the reduced type
	 * name, and an additional field among {@code typeArguments} containing the type arguments of a
	 * parameterized type or the element type of an array type.
	 * 
	 * @param annotatedType
	 * @param gen
	 * @param provider
	 * @throws IOException
	 */
	private void writeTypeDescription(AnnotatedType annotatedType, JsonGenerator gen,
			SerializerProvider provider) throws IOException {
		Type type = annotatedType.getType();
		String reducedTypeName = computeReducedTypeName(type);

		List<Annotation> constraints = selectConstraints(annotatedType.getAnnotations());

		boolean isParametricType = type instanceof ParameterizedType || TypeUtils.isArrayType(type);

		gen.writeStartObject();
		gen.writeStringField("name", reducedTypeName);
		appendConstraints(constraints, gen, provider);

		if (isParametricType) {
			gen.writeArrayFieldStart("typeArguments");

			if (annotatedType instanceof AnnotatedParameterizedType) {
				AnnotatedType[] annotatedTypeArguments = ((AnnotatedParameterizedType) annotatedType)
						.getAnnotatedActualTypeArguments();
				for (AnnotatedType arg : annotatedTypeArguments) {
					writeTypeDescription(arg, gen, provider);
				}
			} else if (TypeUtils.isArrayType(type)) {
				AnnotatedType componentType = ((AnnotatedArrayType) annotatedType)
						.getAnnotatedGenericComponentType();
				writeTypeDescription(componentType, gen, provider);
			}

			gen.writeEndArray();
		}

		gen.writeEndObject();

	}

	private List<Annotation> selectConstraints(Annotation[] annotations) {
		return Arrays.stream(annotations)
				.filter(a -> a.annotationType().isAnnotationPresent(Constraint.class)).collect(toList());
	}

	private void appendConstraints(Collection<? extends Annotation> constraints, JsonGenerator gen,
			SerializerProvider provider) throws IOException {
		if (constraints == null || constraints.isEmpty())
			return;

		gen.writeArrayFieldStart("constraints");

		for (Annotation c : constraints) {
			gen.writeStartObject();
			Class<? extends Annotation> annotationType = c.annotationType();
			gen.writeStringField("@type", annotationType.getName());
			for (Method m : annotationType.getDeclaredMethods()) {
				try {
					gen.writeObjectField(m.getName(), m.invoke(c, (Object[]) null));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					provider.reportMappingProblem(e,
							"Could not serialize attribute \"%s\" in annotation \"%s\"", m.getName(),
							m.getDeclaringClass().getSimpleName());
				}
			}
			gen.writeEndObject();
		}

		gen.writeEndArray();

	}

	/**
	 * Given a {@link Type}, computes its name applying some shortening rules: e.g. java.lang.Boolean --&gt;
	 * boolean
	 * 
	 * @param parType
	 * @return
	 */
	private String computeReducedTypeName(Type parType) {
		String typeName = parType.getTypeName();
		switch (typeName) {
		case "java.lang.Boolean":
			return "boolean";
		case "java.lang.Integer":
			return "integer";
		case "java.lang.Short":
			return "short";
		case "java.lang.Long":
			return "long";
		case "java.lang.Float":
			return "float";
		case "java.lang.Double":
			return "double";
		}

		if (Objects.equals(parType, IRI.class)) {
			return "IRI";
		}

		if (Objects.equals(parType, BNode.class)) {
			return "BNode";
		}

		if (Objects.equals(parType, Resource.class)) {
			return "Resource";
		}

		if (Objects.equals(parType, Literal.class)) {
			return "Literal";
		}

		if (Objects.equals(parType, Value.class)) {
			return "RDFValue";
		}

		if (Objects.equals(parType, URL.class)) {
			return "URL";
		}

		if (Objects.equals(parType, ExtensionSpecificationByRef.class)) {
			return "ExtensionSpecificationByRef";
		}

		Class<?> rawParType = TypeUtils.getRawType(parType, null);

		if (Pair.class.isAssignableFrom(rawParType)) {
			return "Pair";
		}

		if (Objects.equals(rawParType, Set.class)) {
			return "Set";
		}

		if (Objects.equals(rawParType, Map.class)) {
			return "Map";
		}

		if (Objects.equals(rawParType, List.class)) {
			return "List";
		}

		if (TypeUtils.isArrayType(parType)) {
			return "Array";
		}

		return typeName;
	}

}