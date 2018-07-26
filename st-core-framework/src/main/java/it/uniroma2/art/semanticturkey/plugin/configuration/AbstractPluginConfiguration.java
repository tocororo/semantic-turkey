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

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

/**
 * Abstract base class of plugin configuration objects supporting additional properties (in addition to the
 * ones defined as annotated fields).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class AbstractPluginConfiguration extends STPropertiesImpl implements STProperties {

	private static final String field4stringAnnotatedType = "";

	Class<? extends AbstractPluginConfiguration> thisClass;

	protected Map<String, String> additionalConfigurationParameters = new LinkedHashMap<String, String>();

	protected AbstractPluginConfiguration() {
		thisClass = this.getClass();
	}

	@Override
	public Collection<String> getProperties() {
		Collection<String> properties = super.getProperties();
		properties.addAll(additionalConfigurationParameters.keySet());
		return properties;
	}

	@Override
	public String getPropertyDisplayName(String id) throws PropertyNotFoundException {
		try {
			return super.getPropertyDisplayName(id);
		} catch (PropertyNotFoundException e) {
			if (additionalConfigurationParameters.containsKey(id)) {
				return id;
			} else {
				throw new PropertyNotFoundException(String.format("Parameter %s not found", id));
			}
 		}
	}

	@Override
	public Object getPropertyValue(String id) throws PropertyNotFoundException {
		try {
			return super.getPropertyValue(id);
		} catch (PropertyNotFoundException e) {

			boolean additionalPar = additionalConfigurationParameters.containsKey(id);

			if (additionalPar) {
				return additionalConfigurationParameters.get(id);
			} else {
				throw new PropertyNotFoundException(String.format("Parameter %s not found", id));
			}
		} catch (IllegalArgumentException e) {
			throw new PropertyNotFoundException(e);
		}
	}

	@Override
	public void setPropertyValue(String id, Object value) throws WrongPropertiesException {
		try {
			super.setPropertyValue(id, value);
		} catch (WrongPropertiesException e1) {
			Throwable cause = e1.getCause();
			if (cause instanceof IllegalArgumentException) {
				throw e1;
			} else {
				additionalConfigurationParameters.put(id, value.toString());
			}
		}
	}

	@Override
	public AnnotatedType getPropertyAnnotatedType(String id) throws PropertyNotFoundException {
		try {
			return super.getPropertyAnnotatedType(id);
		} catch (PropertyNotFoundException e) {
			if (additionalConfigurationParameters.containsKey(id)) {
				try {
					return this.getClass().getDeclaredField("field4stringAnnotatedType").getAnnotatedType();
				} catch (NoSuchFieldException | SecurityException e1) {
					throw new RuntimeException(
							"The field \"field4stringAnnotatedType\" was erroneously removed from AbstractPluginConfiguration");
				}
			} else {
				throw new PropertyNotFoundException(e);
			}
		}
	}

	@Override
	public String getPropertyContentType(String parID) throws PropertyNotFoundException {
		try {
			return super.getPropertyContentType(parID);
		} catch (PropertyNotFoundException e) {
			if (additionalConfigurationParameters.containsKey(parID)) {
				return null;
			} else {
				throw new PropertyNotFoundException(e);
			}
		}
	}

	@Override
	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		try {
			return super.isRequiredProperty(parID);
		} catch (PropertyNotFoundException e) {
			if (additionalConfigurationParameters.containsKey(parID))
				return false;

			throw e;
		}
	}

	@Override
	public String getPropertyDescription(String id) throws PropertyNotFoundException {
		try {
			return super.getPropertyDescription(id);
		} catch (PropertyNotFoundException e) {
			if (additionalConfigurationParameters.containsKey(id)) {
				return "";
			}

			throw e;
		}
	}
}
