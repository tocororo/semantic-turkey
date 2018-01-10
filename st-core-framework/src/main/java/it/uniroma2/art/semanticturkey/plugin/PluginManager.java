/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
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

package it.uniroma2.art.semanticturkey.plugin;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.plugin.extpts.STOSGIExtension;
import it.uniroma2.art.semanticturkey.properties.STProperties;

/**
 * @author Andrea Turbati
 * @author Armando Stellato
 * 
 */
public class PluginManager {

	protected static Logger logger = LoggerFactory.getLogger(PluginManager.class);
	private static BundleContext m_felix = null;

	private static boolean directAccessTest = false;
	private static Collection<PluginFactory<?, ?, ?, ?, ?>> testPluginFactoryCls = new ArrayList<>();

	public static boolean isDirectAccessTest() {
		return directAccessTest;
	}

	public static void setDirectAccessTest(boolean test) {
		PluginManager.directAccessTest = test;
	}

	public static void setContext(BundleContext toSet) {
		m_felix = toSet;
	}

	public static void setTestPluginFactoryImpls(Collection<PluginFactory<?, ?, ?, ?, ?>> impls) {
		testPluginFactoryCls = impls;
	}

	private static ArrayList<String> getServletExtensionsIDForType(Class<? extends STOSGIExtension> type) {
		ArrayList<String> servletExtensionsList = new ArrayList<String>();

		ServiceTracker m_tracker = null;
		m_tracker = new ServiceTracker(m_felix, type.getName(), null);
		m_tracker.open();

		Object[] services = m_tracker.getServices();
		for (int i = 0; (services != null) && i < services.length; ++i) {
			servletExtensionsList.add(((STOSGIExtension) services[i]).getId());
		}
		m_tracker.close();

		return servletExtensionsList;
	}

	@SuppressWarnings("unchecked")
	private static <T extends STOSGIExtension> ArrayList<T> getServletExtensionsForType(Class<T> type) {
		ArrayList<T> servletExtensionsList = new ArrayList<T>();

		ServiceTracker m_tracker = null;
		m_tracker = new ServiceTracker(m_felix, type.getName(), null);
		m_tracker.open();

		Object[] services = m_tracker.getServices();
		for (int i = 0; (services != null) && i < services.length; ++i) {
			servletExtensionsList.add((T) services[i]);
		}
		m_tracker.close();

		return servletExtensionsList;
	}

	// // New Methods
	public static <T extends STProperties, Q extends STProperties, R extends STProperties, P extends STProperties, S extends STProperties> PluginFactory<T, Q, R, P, S> getPluginFactory(
			String factoryID) {
		// test preamble
		if (isDirectAccessTest()) {
			return (PluginFactory<T, Q, R, P, S>) testPluginFactoryCls.stream()
					.filter(pf -> pf.getID().equals(factoryID)).findFirst().orElse(null);
		} else {
			// real use
			ServiceTracker tracker = new ServiceTracker(m_felix, PluginFactory.class.getName(), null);
			tracker.open();
			PluginFactory<T, Q, R, P, S> repImpl = null;
			Object[] services = tracker.getServices();
			for (int i = 0; (services != null) && i < services.length; ++i) {
				if (((PluginFactory<T, Q, R, P, S>) services[i]).getID().equals(factoryID)) {
					repImpl = (PluginFactory<T, Q, R, P, S>) services[i];
					break;
				}
			}
			tracker.close();
			return repImpl;
		}
	}

	public static Collection<PluginFactory<?, ?, ?, ?, ?>> getPluginFactories(String extensionPoint) {
		ServiceTracker tracker = null;
		try {
			tracker = new ServiceTracker(m_felix,
					m_felix.createFilter(String.format(
							"(&(objectClass=%s)(it.uniroma2.art.semanticturkey.extensionpoint=%s))",
							PluginFactory.class.getName(), extensionPoint)),
					null);
		} catch (InvalidSyntaxException e) {
			new RuntimeException("This should have never happened", e);
		}

		tracker.open();
		Collection<PluginFactory<?, ?, ?, ?, ?>> pluginFactories = new ArrayList<PluginFactory<?, ?, ?, ?, ?>>();
		Object[] services = tracker.getServices();
		for (int i = 0; (services != null) && i < services.length; ++i) {
			pluginFactories.add((PluginFactory<?, ?, ?, ?, ?>) services[i]);
		}
		tracker.close();
		return pluginFactories;
	}
}
