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
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.ConfParameterNotFoundException;
import it.uniroma2.art.semanticturkey.plugin.configuration.ContentType;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;
import it.uniroma2.art.semanticturkey.plugin.configuration.RequiredConfigurationParameter;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;


@GenerateSTServiceController
@Validated
@Component
public class Plugins extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Plugins.class);

	/**
	 * Returns the available configuration options for a given plug-in
	 * @param factoryID the identifier of the plug-in factory
	 * @return
	 * @throws ConfParameterNotFoundException
	 */
	@GenerateSTServiceController
	public Response getPluginConfigurations(String factoryID) throws ConfParameterNotFoundException  {
		PluginFactory<?> pluginFactory = PluginManager.getPluginFactory(factoryID);
		
		Collection<PluginConfiguration> mConfs = pluginFactory.getPluginConfigurations();
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		
		Element dataElement = response.getDataElement();

		for (PluginConfiguration mConf : mConfs) {

			Element newConfType = XMLHelp.newElement(dataElement, "configuration");

			newConfType.setAttribute("type", mConf.getClass().getName());

			newConfType.setAttribute("shortName", mConf.getShortName());

			newConfType.setAttribute("editRequired", Boolean.toString(mConf.hasRequiredParameters()));

			Collection<String> pars = mConf.getConfigurationParameters();

			for (String par : pars) {
				String parDescr = mConf.getParameterDescription(par);
				Element newPar = XMLHelp.newElement(newConfType, "par");
				newPar.setAttribute("name", par);
				newPar.setAttribute("description", parDescr);
				newPar.setAttribute("required", Boolean.toString(mConf.isRequiredParameter(par)));
				String contentType = mConf.getParameterContentType(par);
				if (contentType != null)
					newPar.setAttribute("type", contentType);
				Object parValue = mConf.getParameterValue(par);
				if (parValue != null)
					newPar.setTextContent(parValue.toString());
			}

		}

		return response;
	}
	
	@GenerateSTServiceController
	public Response getAvailablePlugins(String extensionPoint) throws ConfParameterNotFoundException {
		Collection<PluginFactory<?>> pluginFactoryCollection = PluginManager.getPluginFactories(extensionPoint);
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element data = response.getDataElement();
		
			
		for (PluginFactory<?> pluginFactory : pluginFactoryCollection) {
			Element pluginElement = XMLHelp.newElement(data, "plugin");
			pluginElement.setAttribute("factoryID", pluginFactory.getClass().getName());
		}
		
		return response;
	}
}
