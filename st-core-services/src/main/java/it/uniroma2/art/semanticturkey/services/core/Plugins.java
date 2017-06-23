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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.models.conf.ConfParameterNotFoundException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
public class Plugins extends STServiceAdapterOLD {

	protected static Logger logger = LoggerFactory.getLogger(Plugins.class);

	/**
	 * Returns the available configuration options for a given plug-in
	 * 
	 * @param factoryID
	 *            the identifier of the plug-in factory
	 * @return
	 * @throws PropertyNotFoundException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(plugins)', 'R')")
	public Response getPluginConfigurations(String factoryID) throws PropertyNotFoundException {
		PluginFactory<?, ?, ?> pluginFactory = PluginManager.getPluginFactory(factoryID);

		Collection<STProperties> mConfs = pluginFactory.getPluginConfigurations();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);

		Element dataElement = response.getDataElement();

		for (STProperties mConf : mConfs) {

			Element newConfType = XMLHelp.newElement(dataElement, "configuration");

			newConfType.setAttribute("type", mConf.getClass().getName());

			newConfType.setAttribute("shortName", mConf.getShortName());

			newConfType.setAttribute("editRequired", Boolean.toString(mConf.hasRequiredProperties()));

			Collection<String> props = mConf.getProperties();

			for (String prop : props) {
				String parDescr = mConf.getPropertyDescription(prop);
				Element newPar = XMLHelp.newElement(newConfType, "par");
				newPar.setAttribute("name", prop);
				newPar.setAttribute("description", parDescr);
				newPar.setAttribute("required", Boolean.toString(mConf.isRequiredProperty(prop)));
				String contentType = mConf.getPropertyContentType(prop);
				if (contentType != null)
					newPar.setAttribute("type", contentType);
				Object parValue = mConf.getPropertyValue(prop);
				if (parValue != null)
					newPar.setTextContent(parValue.toString());
			}

		}

		return response;
	}

	/**
	 * Returns the implementations of a given extension point
	 * 
	 * @param extensionPoint
	 *            the name of the extension point (it should be the fully qualified name of the interface
	 *            implemented by the plug-in instances)
	 * @return
	 * @throws ConfParameterNotFoundException
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(plugins)', 'R')")
	public Response getAvailablePlugins(String extensionPoint) throws ConfParameterNotFoundException {
		Collection<PluginFactory<?, ?, ?>> pluginFactoryCollection = PluginManager
				.getPluginFactories(extensionPoint);

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element data = response.getDataElement();

		for (PluginFactory<?, ?, ?> pluginFactory : pluginFactoryCollection) {
			Element pluginElement = XMLHelp.newElement(data, "plugin");
			pluginElement.setAttribute("factoryID", pluginFactory.getClass().getName());
		}

		return response;
	}
}
