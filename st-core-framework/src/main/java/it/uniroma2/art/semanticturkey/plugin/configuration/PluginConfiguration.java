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
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

public interface PluginConfiguration {

	/**
	 * returns all the parameters of the implementing PluginConfiguration subclass, which have been annotated
	 * as {@link PluginConfigurationParameter}
	 * 
	 * @return
	 */
	public abstract Collection<String> getConfigurationParameters();

	/**
	 * gets the value of a parameter previously set on this configuration
	 * 
	 * @param id
	 * @return
	 * @throws ConfParameterNotFoundException
	 */
	public abstract Object getParameterValue(String id) throws ConfParameterNotFoundException;

	/**
	 * sets the value of the Configuration Parameter to <code>value</code><br/>
	 * Note that it is possible to pass the value both with the proper type, if it is known in advance, as
	 * well with a generic String, which will be evaluated and converted to the appropriate type. Supported
	 * types are now:
	 * <ul>
	 * <li>Boolean</li>
	 * <li>Integer</li>
	 * <li>Long</li>
	 * <li>Double</li>
	 * </ul>
	 * 
	 * @param id
	 * @param value
	 * @throws BadConfigurationException
	 */
	public abstract void setParameter(String id, Object value) throws BadConfigurationException;

	/**
	 * invokes {@link #setParameter(String, Object)} on each of the parameter/value pairs found in
	 * <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws BadConfigurationException
	 * @throws IOException
	 */
	public abstract void loadParameters(File propertyFile) throws BadConfigurationException, IOException;

	/**
	 * invokes {@link #setParameter(String, Object)} on each of the parameter/value pairs found in
	 * <code>properties</code>
	 * 
	 * @param properties
	 * @throws BadConfigurationException
	 */
	public abstract void setParameters(Properties properties) throws BadConfigurationException;

	/**
	 * stores the parameters of this configuration in file <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws IOException
	 * @throws BadConfigurationException
	 */
	public abstract void storeParameters(File propertyFile) throws IOException, BadConfigurationException;

	/**
	 * get the expected type of content for the parameter. Can be used by external tools to drive the
	 * acquisition of that value or to check
	 * 
	 * @param parID
	 * @return
	 * @throws ConfParameterNotFoundException
	 */
	public abstract String getParameterContentType(String parID) throws ConfParameterNotFoundException;

	/**
	 * tells if this PluginConfiguration needs to be explicitly set by the user or if it can be used in its
	 * default settings
	 * 
	 * @return
	 */
	public abstract boolean hasRequiredParameters();

	public abstract boolean isRequiredParameter(String parID) throws ConfParameterNotFoundException;

	/**
	 * this method is useful when OWL ART API are embedded inside tools presenting list of configuration
	 * parameters for being set by the users, to provide hints for the user on which kind of values should be
	 * used to fill them
	 * 
	 * @param id
	 * @return
	 * @throws ConfParameterNotFoundException
	 */
	public abstract String getParameterDescription(String id) throws ConfParameterNotFoundException;

	/**
	 * @return a humanly understandable short name representing the type of this model configuration
	 */
	public abstract String getShortName();

}