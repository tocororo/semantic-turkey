package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

public abstract class STPropertiesImpl implements STProperties {

	Class<? extends STPropertiesImpl> thisClass;

	protected STPropertiesImpl() {
		thisClass = this.getClass();
	}

	protected STPropertiesImpl(File propertyFile) throws IOException, WrongPropertiesException {
		this();
		loadProperties(propertyFile);
	}

	public Collection<String> getProperties() {
		Collection<String> properties = new ArrayList<String>();

		Field[] fields = thisClass.getFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(STProperty.class))
				properties.add(field.getName());
		}
		return properties;
	}

	public Object getPropertyValue(String id) throws PropertyNotFoundException {
		try {
			Field property = thisClass.getField(id);
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

	public void setPropertyValue(String id, Object value) throws WrongPropertiesException {

		Field prop = null;
		try {
			prop = thisClass.getField(id);

			// System.out.println("generic type for Prop: " + prop.getGenericType());

			if ((value.getClass() == String.class) && (prop.getGenericType() != String.class)) {
				value = convertToPropertValue(prop, value);
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

	public void setProperties(Properties props) throws WrongPropertiesException {
		Enumeration<?> propNames = props.propertyNames();
		while (propNames.hasMoreElements()) {
			String propName = propNames.nextElement().toString();
			setPropertyValue(propName, props.getProperty(propName));
		}
	}

	public void loadProperties(File propertyFile) throws WrongPropertiesException, IOException {
		Properties props = new java.util.Properties();
		FileReader fileReader = new FileReader(propertyFile);
		props.load(fileReader);
		setProperties(props);
		fileReader.close();
	}

	public void storeProperties(File propertyFile) throws IOException, WrongPropertiesException {
		Properties props = new java.util.Properties();
		try (FileWriter fileWriter = new FileWriter(propertyFile)) {
			storeProperties(props);
			props.store(fileWriter, "list of properties");
		}
	}

	public void storeProperties(Properties properties) throws IOException, WrongPropertiesException {
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

	public String getPropertyContentType(String parID) throws PropertyNotFoundException {
		try {
			Field field = thisClass.getField(parID);

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

	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		try {
			Field field = thisClass.getField(parID);

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

	public boolean hasRequiredProperties() {
		Field[] fields = thisClass.getFields();
		for (int i = 0; i < fields.length; i++)
			if (fields[i].isAnnotationPresent(Required.class))
				return true;
		return false;
	}

	public String getPropertyDescription(String id) throws PropertyNotFoundException {
		try {
			Field field = thisClass.getField(id);
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

	private Object convertToPropertValue(Field prop, Object value) {
		if (prop.getGenericType() == Boolean.class || prop.getGenericType() == boolean.class) {
			value = Boolean.parseBoolean((String) value);
		} else if (prop.getGenericType() == Long.class || prop.getGenericType() == long.class)
			value = Long.parseLong((String) value);
		else if (prop.getGenericType() == Integer.class || prop.getGenericType() == int.class)
			value = Integer.parseInt((String) value);
		else if (prop.getGenericType() == Double.class || prop.getGenericType() == double.class)
			value = Double.parseDouble((String) value);
		return value;
	}

	public String toString() {
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

}
