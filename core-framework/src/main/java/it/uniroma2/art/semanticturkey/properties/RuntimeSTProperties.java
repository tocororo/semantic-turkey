package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.reflect.AbstractInvocationHandler;

/**
 * An {@link STProperties} which can be defined at runtime. Differently from ordinary {@link STProperties}
 * implementations that define their content as Java fields, this class allows to define the allowed
 * properties at runtime via an API.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RuntimeSTProperties implements STProperties {

	public static class PropertyDefinition {

		private AnnotatedType annotatedType;
		private String contentType;
		private String description;
		private String displayName;
		private Optional<Collection<String>> enumeration;
		private boolean required;
		private List<Annotation> annotations;

		public PropertyDefinition(String displayName, String description, boolean required,
				AnnotatedType annotatedType) {
			this.annotatedType = annotatedType;
			this.displayName = displayName;
			this.description = description;
			this.required = required;
			this.contentType = null;
			this.enumeration = Optional.empty();
			this.annotations = new ArrayList<>();
		}

		public void setAnnotatedType(AnnotatedType annotatedType) {
			this.annotatedType = annotatedType;
		}

		public AnnotatedType getAnnotatedType() {
			return annotatedType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Nullable
		public String getContentType() {
			return contentType;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public void setEnumeration(String... items) {
			if (items.length == 0) {
				this.enumeration = Optional.empty();
			} else {
				this.enumeration = Optional.of(Collections.unmodifiableList(Arrays.asList(items)));
			}
		}

		public Optional<Collection<String>> getEnumeration() {
			return enumeration;
		}

		public Annotation[] getAnnotations() {
			return annotations.toArray(new Annotation[annotations.size()]);
		}

		public void addAnnotation(Class<? extends Annotation> annotationClazz, Map<String, Object> values) {

			// uses Guava's abstract implementation supporting equals, hashCode, toString
			Annotation annot = (Annotation) Proxy.newProxyInstance(this.getClass().getClassLoader(),
					new Class<?>[] { annotationClazz }, new AbstractInvocationHandler() {

						@Override
						protected Object handleInvocation(Object proxy, Method method, Object[] args)
								throws Throwable {
							if (method.getDeclaringClass() == Annotation.class
									&& method.getName().equals("annotationType")) {
								return annotationClazz;
							} else {
								if (values.containsKey(method.getName())) {
									return values.get(method.getName());
								} else {
									try {
										return method.getDefaultValue();
									} catch (TypeNotPresentException e) {
										throw new IllegalStateException("Missing value for member "
												+ method.getName() + " of annotation "
												+ annotationClazz.getSimpleName());
									}
								}
							}
						}
					});

			annotations.add(annot);
		}

	}

	@FunctionalInterface
	public interface Buildable {
		AnnotatedType build();
	}

	public static class AnnotatedTypeBuilder implements Buildable {
		private Class<?> clazz;
		private Collection<Buildable> typeArgumentBuilders;
		private Collection<Annotation> annotations;

		public AnnotatedTypeBuilder() {
			this.clazz = null;
			this.typeArgumentBuilders = new ArrayList<>();
			this.annotations = new ArrayList<>();
		}

		public AnnotatedTypeBuilder withType(Class<?> clazz) {
			this.clazz = clazz;
			return this;
		}

		public AnnotatedTypeBuilder withAnnotation(Class<? extends Annotation> annotationClazz,
				Map<String, Object> values) {

			// uses Guava's abstract implementation supporting equals, hashCode, toString
			Annotation annot = (Annotation) Proxy.newProxyInstance(this.getClass().getClassLoader(),
					new Class<?>[] { annotationClazz }, new AbstractInvocationHandler() {

						@Override
						protected Object handleInvocation(Object proxy, Method method, Object[] args)
								throws Throwable {
							if (method.getDeclaringClass() == Annotation.class
									&& method.getName().equals("annotationType")) {
								return annotationClazz;
							} else {
								return values.get(method.getName());
							}
						}
					});

			annotations.add(annot);

			return this;
		}

		public AnnotatedTypeBuilder withTypeArgument(AnnotatedTypeBuilder typeArgument) {
			typeArgumentBuilders.add(typeArgument);
			return this;
		}

		public AnnotatedTypeBuilder withTypeArgument(AnnotatedType typeArgument) {
			typeArgumentBuilders.add(() -> typeArgument);
			return this;
		}

		public AnnotatedType build() {
			if (clazz == null) {
				throw new IllegalStateException("Base type not defined");
			}

			AnnotatedType annotatedType;

			if (typeArgumentBuilders.size() > 0) {
				List<AnnotatedType> annotatedTypeArguments = typeArgumentBuilders.stream()
						.map(Buildable::build).collect(Collectors.toList());
				Type[] typeArguments = annotatedTypeArguments.stream().map(AnnotatedType::getType)
						.collect(Collectors.toList()).toArray(new Type[0]);

				Type type = TypeUtils.parameterize(clazz, typeArguments);

				annotatedType = new AnnotatedParameterizedType() {

					@Override
					public Annotation[] getDeclaredAnnotations() {
						return annotations.toArray(new Annotation[annotations.size()]);
					}

					@Override
					public Annotation[] getAnnotations() {
						return this.getDeclaredAnnotations();
					}

					@SuppressWarnings("unchecked")
					@Override
					public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
						return (T) Arrays.stream(getAnnotations()).filter(annotationClass::isInstance)
								.findAny().orElse(null);
					}

					@Override
					public Type getType() {
						return type;
					}

					@Override
					public AnnotatedType[] getAnnotatedActualTypeArguments() {
						return annotatedTypeArguments
								.toArray(new AnnotatedType[annotatedTypeArguments.size()]);
					}
				};
			} else {
				Type type = clazz;
				annotatedType = new AnnotatedType() {

					@Override
					public Annotation[] getDeclaredAnnotations() {
						return annotations.toArray(new Annotation[annotations.size()]);
					}

					@Override
					public Annotation[] getAnnotations() {
						return this.getDeclaredAnnotations();
					}

					@SuppressWarnings("unchecked")
					@Override
					public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
						return (T) Arrays.stream(getAnnotations()).filter(annotationClass::isInstance)
								.findAny().orElse(null);
					}

					@Override
					public Type getType() {
						return type;
					}
				};
			}

			return annotatedType;
		}
	}

	private String shortName;
	private String htmlDescription;
	private String htmlWarning;
	private Map<String, org.apache.commons.lang3.tuple.Pair<PropertyDefinition, Object>> properties;

	public RuntimeSTProperties(String shortName) {
		this.shortName = shortName;
		this.properties = new LinkedHashMap<>();
	}

	protected Pair<PropertyDefinition, Object> getPropertyDefinitionAndValue(String id)
			throws PropertyNotFoundException {
		Pair<PropertyDefinition, Object> defAndValue = properties.get(id);

		if (defAndValue == null) {
			throw new PropertyNotFoundException("Poperty \"" + id + "\" is not defined");
		}
		return defAndValue;
	}

	public void addProperty(String id, PropertyDefinition def) {
		properties.put(id, MutablePair.of(def, null));
	}

	public void removeProperty(String id) {
		properties.remove(id);
	}

	public PropertyDefinition getPropertyDefinition(String id) throws PropertyNotFoundException {
		return getPropertyDefinitionAndValue(id).getLeft();
	}

	@Override
	public String getShortName() {
		return shortName;
	}

	public void setHtmlDescription(String htmlDescription) {
		this.htmlDescription = htmlDescription;
	}

	@Override
	@Nullable
	public String getHTMLDescription() {
		return htmlDescription;
	}

	public void setHtmlWarning(String htmlWarning) {
		this.htmlWarning = htmlWarning;
	}

	@Override
	@Nullable
	public String getHTMLWarning() {
		return htmlWarning;
	}

	@Override
	public Collection<String> getProperties() {
		return Collections.unmodifiableSet(properties.keySet());
	}

	@Override
	public AnnotatedType getPropertyAnnotatedType(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getAnnotatedType();
	}

	@Override
	public String getPropertyContentType(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getContentType();
	}

	@Override
	public String getPropertyDescription(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getDescription();
	}

	@Override
	public String getPropertyDisplayName(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getDisplayName();
	}

	@Override
	public Optional<Collection<String>> getEnumeration(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getEnumeration();
	}

	@Override
	public Object getPropertyValue(String id) throws PropertyNotFoundException {
		return getPropertyDefinitionAndValue(id).getRight();
	}

	@Override
	public void setPropertyValue(String id, Object value) throws WrongPropertiesException {
		try {
			AnnotatedType annotType = getPropertyAnnotatedType(id);
			Object convertedValue = checkAndConvertPropertyValue(id, value, annotType.getType());
			getPropertyDefinitionAndValue(id).setValue(convertedValue);
		} catch (PropertyNotFoundException e) {
			throw new WrongPropertiesException(e);
		}
	}

	@Override
	public boolean isRequiredProperty(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).isRequired();
	}

	@Override
	public boolean hasRequiredProperties() {
		for (String p : getProperties()) {
			try {
				if (isRequiredProperty(p))
					return true;
			} catch (PropertyNotFoundException e) {
				// nothing to do. This should never happen
			}
		}

		return false;
	}

	@Override
	public Annotation[] getAnnotations(String id) throws PropertyNotFoundException {
		return getPropertyDefinition(id).getAnnotations();
	}
}
