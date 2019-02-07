package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;

/**
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 *
 */
@JsonSerialize(using = STPropertiesSerializer.class)
public interface STProperties {

	/**
	 * @return a humanly understandable short name representing the type of this property set
	 */
	String getShortName();

	/**
	 * An optional HTML description of this type of propertySet
	 * 
	 * @return an HTML description of this type of propertySet. It may be <code>null</code>, if no description
	 *         is available
	 */
	default String getHTMLDescription() {
		return null;
	}

	/**
	 * An optional HTML warning related to this type of propertySet
	 * 
	 * @return An optional HTML warning related to this type of propertySet. It may be <code>null</code>, if
	 *         no warning is available
	 */
	default String getHTMLWarning() {
		return null;
	}

	/**
	 * returns all the properties of the class implementing this interface, which have been annotated as
	 * {@link STProperty}
	 * 
	 * @return
	 */
	default Collection<String> getProperties() {
		Collection<String> properties = new ArrayList<String>();

		Field[] fields = this.getClass().getFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(STProperty.class))
				properties.add(field.getName());
		}
		return properties;
	}

	/**
	 * gets the value of a property previously set on this set of properties
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	default Object getPropertyValue(String id) throws PropertyNotFoundException {
		try {
			Field property = this.getClass().getField(id);
			Object returnedValue = property.get(this);
			return returnedValue;
		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		} catch (IllegalArgumentException e) {
			throw new PropertyNotFoundException(e);
		} catch (IllegalAccessException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	default Type getPropertyType(String id) throws PropertyNotFoundException {
		return getPropertyAnnotatedType(id).getType();
	}

	default AnnotatedType getPropertyAnnotatedType(String id) throws PropertyNotFoundException {
		try {
			Field property = this.getClass().getField(id);
			return property.getAnnotatedType();
		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		} catch (IllegalArgumentException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	default void setPropertyValue(String id, Object value) throws WrongPropertiesException {

		Field prop = null;
		try {
			prop = this.getClass().getField(id);

			// System.out.println("generic type for Prop: " + prop.getGenericType());

			if ((value.getClass() == String.class) && (prop.getGenericType() != String.class)) {
				value = convertToPropertValue(prop.getGenericType(), value);
			}

			it.uniroma2.art.semanticturkey.properties.Enumeration enumeration = prop
					.getAnnotation(it.uniroma2.art.semanticturkey.properties.Enumeration.class);

			if (enumeration != null) {
				Object tempValue = value;

				Arrays.stream(enumeration.value()).filter(v -> v.equals(tempValue.toString())).findAny()
						.orElseThrow(() -> new IllegalArgumentException("Value \"" + tempValue.toString()
								+ "\" is not assignable to property \"" + id + "\""));
			}
			prop.set(this, value);
		} catch (SecurityException e) {
			throw new WrongPropertiesException(e);
		} catch (NoSuchFieldException e) {
			throw new WrongPropertiesException("there is no property called: " + id);
		} catch (IllegalArgumentException e) {
			throw new WrongPropertiesException(e);
		} catch (IllegalAccessException e) {
			throw new WrongPropertiesException(e);
		}
	}

	/**
	 * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
	 * <code>properties</code>
	 * 
	 * @param properties
	 * @throws WrongPropertiesException
	 */
	default void setProperties(Properties props) throws WrongPropertiesException {
		Enumeration<?> propNames = props.propertyNames();
		while (propNames.hasMoreElements()) {
			String propName = propNames.nextElement().toString();
			setPropertyValue(propName, props.getProperty(propName));
		}
	}

	/**
	 * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
	 * <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws WrongPropertiesException
	 * @throws IOException
	 */
	default void loadProperties(File propertyFile) throws WrongPropertiesException, IOException {
		Properties props = new java.util.Properties();
		FileReader fileReader = new FileReader(propertyFile);
		props.load(fileReader);
		setProperties(props);
		fileReader.close();
	}

	/**
	 * stores the properties in this instance in file <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws IOException
	 * @throws WrongPropertiesException
	 */
	default void storeProperties(File propertyFile) throws IOException, WrongPropertiesException {
		Properties props = new java.util.Properties();
		try (FileWriter fileWriter = new FileWriter(propertyFile)) {
			storeProperties(props);
			props.store(fileWriter, "list of properties");
		}
	}

	/**
	 * stores the properties in this instance in the given {@link Properties} object <code>properties</code>
	 * 
	 * @param propertyFile
	 * @throws IOException
	 * @throws WrongPropertiesException
	 */
	default void storeProperties(Properties properties) throws IOException, WrongPropertiesException {
		Collection<String> pars = getProperties();
		try {
			for (String par : pars) {
				Object value = getPropertyValue(par);
				if (value == null)
					continue; // skip null values
				properties.setProperty(par, value.toString());
			}
		} catch (PropertyNotFoundException e) {
			throw new WrongPropertiesException(e);
		}

	}

	/**
	 * get the expected type of content for the property. Can be used by external tools to drive the
	 * acquisition of that value or to check
	 * 
	 * @param parID
	 * @return
	 * @throws PropertyNotFoundException
	 */
	default String getPropertyContentType(String parID) throws PropertyNotFoundException {
		try {
			Field field = this.getClass().getField(parID);

			if (!field.isAnnotationPresent(ContentType.class))
				return null;

			ContentType annotation = field.getAnnotation(ContentType.class);
			return annotation.value();

		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	default boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		try {
			Field field = this.getClass().getField(parID);

			if (field.isAnnotationPresent(Required.class))
				return true;
			else
				return false;

		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	/**
	 * tells if this set of properties needs to be explicitly set by the user or if it can be used in its
	 * default settings
	 * 
	 * @return
	 */
	default boolean hasRequiredProperties() {
		Field[] fields = this.getClass().getFields();
		for (int i = 0; i < fields.length; i++)
			if (fields[i].isAnnotationPresent(Required.class))
				return true;
		return false;
	}

	/**
	 * this method returns the description of a property
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	default String getPropertyDescription(String id) throws PropertyNotFoundException {
		try {
			Field field = this.getClass().getField(id);
			if (field.isAnnotationPresent(STProperty.class))
				return ((STProperty) field.getAnnotation(STProperty.class)).description();
			else
				throw new PropertyNotFoundException("Property: " + id + " not found");
		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	/**
	 * this method returns the displayName of a property. If not provided, return the property name
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	default String getPropertyDisplayName(String id) throws PropertyNotFoundException {
		try {
			Field field = this.getClass().getField(id);
			if (field.isAnnotationPresent(STProperty.class)) {
				String propValue = ((STProperty) field.getAnnotation(STProperty.class)).displayName();
				if (propValue.equals("")) { // not provided, empty string is the default
					return id;
				} else {
					return propValue;
				}
			} else
				throw new PropertyNotFoundException("Property: " + id + " not found");
		} catch (SecurityException e) {
			throw new PropertyNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	default Object convertToPropertValue(Type type, Object value) {
		if (type == Boolean.class || type == boolean.class) {
			value = Boolean.parseBoolean((String) value);
		} else if (type == Long.class || type == long.class)
			value = Long.parseLong((String) value);
		else if (type == Integer.class || type == int.class)
			value = Integer.parseInt((String) value);
		else if (type == Double.class || type == double.class)
			value = Double.parseDouble((String) value);
		else if(TypeUtils.isAssignable(type, Value.class)) {
			value = NTriplesUtil.parseValue((String)value, SimpleValueFactory.getInstance());
			if (!((Class<?>)type).isInstance(value)) {
				throw new IllegalArgumentException("The provided value \"" + value + "\" is not a serialization of an RDF4J " + ((Class<?>)type).getSimpleName());
			}

		}
		return value;
	}

	default String getStringRepresentation() {
		Collection<String> pars = getProperties();
		StringBuffer stringed = new StringBuffer(
				"STProperties [" + this.getClass().getCanonicalName() + "\n");
		for (String par : pars) {
			String value;
			try {
				value = getPropertyValue(par).toString();
			} catch (PropertyNotFoundException e) {
				value = "propNotFound!";
			}
			stringed.append(par + ": " + value + "\n");
		}
		stringed.append("]");
		return stringed.toString();
	}

	/**
	 * Tells if the property with given {@code id} is enumerated (see {@link Enumeration}).
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	default boolean isEnumerated(String id) throws PropertyNotFoundException {
		try {
			Field field = this.getClass().getField(id);
			return field.isAnnotationPresent(it.uniroma2.art.semanticturkey.properties.Enumeration.class);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	/**
	 * Returns the enumeration (if any) associated with the property with given {@code id}.
	 * 
	 * @param id
	 * @return
	 */
	default Optional<Collection<String>> getEnumeration(String id) throws PropertyNotFoundException {
		getPropertyDescription(id); // just used to check the existence of the property
		try {
			Field field = this.getClass().getField(id);
			return Optional
					.ofNullable(
							field.getAnnotation(it.uniroma2.art.semanticturkey.properties.Enumeration.class))
					.map(ann -> Lists.newArrayList(ann.value()));
		} catch (NoSuchFieldException | SecurityException e) {
			return Optional.empty();
		}
	}
}
