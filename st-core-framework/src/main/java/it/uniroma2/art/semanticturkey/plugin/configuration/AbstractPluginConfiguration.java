/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is ART OWL API.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * The ART OWL API were developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ART OWL API can be obtained at 
 * http://art.uniroma2.it/owlart
 *
 */

package it.uniroma2.art.semanticturkey.plugin.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

public abstract class AbstractPluginConfiguration implements PluginConfiguration {

	Class<? extends AbstractPluginConfiguration> thisClass;

	protected AbstractPluginConfiguration() {
		thisClass = this.getClass();
	}

	protected AbstractPluginConfiguration(File propertyFile) throws IOException, BadConfigurationException {
		this();
		loadParameters(propertyFile);
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#getConfigurationParameters()
	 */
	public Collection<String> getConfigurationParameters() {
		Collection<String> configurationParameters = new ArrayList<String>();

		Field[] fields = thisClass.getFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(PluginConfigurationParameter.class))
				configurationParameters.add(field.getName());
		}
		return configurationParameters;
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#getParameterValue(java.lang.String)
	 */
	public Object getParameterValue(String id) throws ConfParameterNotFoundException {
		try {
			Field parameter = thisClass.getField(id);
			Object returnedValue = parameter.get(this);
			return returnedValue;
		} catch (SecurityException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (IllegalArgumentException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (IllegalAccessException e) {
			throw new ConfParameterNotFoundException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#setParameter(java.lang.String, java.lang.Object)
	 */
	public void setParameter(String id, Object value) throws BadConfigurationException {

		Field prop = null;
		try {
			prop = thisClass.getField(id);

			// System.out.println("generic type for Prop: " + prop.getGenericType());

			if ((value.getClass() == String.class) && (prop.getGenericType() != String.class)) {
				value = convertToPropertValue(prop, value);
			}

			prop.set(this, value);
		} catch (SecurityException e) {
			throw new BadConfigurationException(e);
		} catch (NoSuchFieldException e) {
			throw new BadConfigurationException("there is no model configuration property called: " + id);
		} catch (IllegalArgumentException e) {
			throw new BadConfigurationException(e);
		} catch (IllegalAccessException e) {
			throw new BadConfigurationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#setParameters(java.util.Properties)
	 */
	public void setParameters(Properties props) throws BadConfigurationException {
		Enumeration<?> propNames = props.propertyNames();
		while (propNames.hasMoreElements()) {
			String propName = propNames.nextElement().toString();
			setParameter(propName, props.getProperty(propName));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#loadParameters(java.io.File)
	 */
	public void loadParameters(File propertyFile) throws BadConfigurationException, IOException {
		Properties props = new java.util.Properties();
		FileReader fileReader = new FileReader(propertyFile);
		props.load(fileReader);
		setParameters(props);
		fileReader.close();
	}

	/*
	 * (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration#storeParameters(java.io.File)
	 */
	public void storeParameters(File propertyFile) throws IOException, BadConfigurationException {
		Properties props = new java.util.Properties();
		FileWriter fileWriter = new FileWriter(propertyFile);
		try {
			Collection<String> pars = getConfigurationParameters();
			for (String par : pars) {
				Object value = getParameterValue(par);
				props.setProperty(par, value.toString());
			}
			props.store(fileWriter, "list of model configuration properties");
		} catch (ConfParameterNotFoundException e) {
			throw new BadConfigurationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.owlart.models.conf.ModelConfiguration#getParameterContentType(java.lang.String)
	 */
	public String getParameterContentType(String parID) throws ConfParameterNotFoundException {
		try {
			Field field = thisClass.getField(parID);

			if (!field.isAnnotationPresent(ContentType.class))
				return null;

			ContentType annotation = field.getAnnotation(ContentType.class);
			return annotation.value();

		} catch (SecurityException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new ConfParameterNotFoundException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.owlart.models.conf.ModelConfiguration#isRequiredParameter(java.lang.String)
	 */
	public boolean isRequiredParameter(String parID) throws ConfParameterNotFoundException {
		try {
			Field field = thisClass.getField(parID);

			if (field.isAnnotationPresent(RequiredConfigurationParameter.class))
				return true;
			else
				return false;

		} catch (SecurityException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new ConfParameterNotFoundException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.owlart.models.conf.ModelConfiguration#hasRequiredParameters()
	 */
	public boolean hasRequiredParameters() {
		Field[] fields = thisClass.getFields();
		for (int i = 0; i < fields.length; i++)
			if (fields[i].isAnnotationPresent(RequiredConfigurationParameter.class))
				return true;
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.uniroma2.art.owlart.models.conf.ModelConfiguration#getParameterDescription(java.lang.String)
	 */
	public String getParameterDescription(String id) throws ConfParameterNotFoundException {
		try {
			Field field = thisClass.getField(id);
			if (field.isAnnotationPresent(PluginConfigurationParameter.class))
				return ((PluginConfigurationParameter) field.getAnnotation(PluginConfigurationParameter.class))
						.description();
			else
				throw new ConfParameterNotFoundException("Parameter: " + id + " not found");
		} catch (SecurityException e) {
			throw new ConfParameterNotFoundException(e);
		} catch (NoSuchFieldException e) {
			throw new ConfParameterNotFoundException(e);
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

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		Collection<String> pars = getConfigurationParameters();
		StringBuffer stringed = new StringBuffer("Plugin Configuration ["
				+ this.getClass().getCanonicalName() + "\n");
		for (String par : pars) {
			String value;
			try {
				value = getParameterValue(par).toString();
			} catch (ConfParameterNotFoundException e) {
				value = "parNotFound!";
			}
			stringed.append(par + ": " + value + "\n");
		}
		stringed.append("]");
		return stringed.toString();
	}

}
