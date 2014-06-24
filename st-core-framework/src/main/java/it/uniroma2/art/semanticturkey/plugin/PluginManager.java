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

import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerFactory;
import it.uniroma2.art.semanticturkey.plugin.extpts.PluginInterface;
import it.uniroma2.art.semanticturkey.plugin.extpts.STOSGIExtension;

import java.util.ArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrea Turbati
 * @author Armando Stellato
 * 
 */
public class PluginManager {

	protected static Logger logger = LoggerFactory.getLogger(PluginManager.class);
	private static BundleContext m_felix = null;

	private static boolean directAccessTest = false;
	private static Class<? extends OntologyManagerFactory<ModelConfiguration>> testOntManagerFactoryCls;

	public static boolean isDirectAccessTest() {
		return directAccessTest;
	}

	public static void setDirectAccessTest(boolean test) {
		PluginManager.directAccessTest = test;
	}
	public static void setContext(BundleContext toSet)
	{
		m_felix=toSet;
	}
	public static void setTestOntManagerFactoryImpl(
			Class<? extends OntologyManagerFactory<ModelConfiguration>> ontmgrcls) {
		testOntManagerFactoryCls = ontmgrcls;
	}

	/**
	 * this method retrieves all IDs of available {@link PluginInterface} implementations installed in the
	 * OSGi framework
	 * 
	 * @return Arraylist
	 */
	public static ArrayList<String> getPluginsIDs() {
		return getServletExtensionsIDForType(PluginInterface.class);
	}

	/**
	 * this method retrieves all available {@link PluginInterface} implementations installed in the OSGi
	 * framework
	 * 
	 * @return
	 */
	public static ArrayList<PluginInterface> getPlugins() {
		return getServletExtensionsForType(PluginInterface.class);
	}

	/**
	 * this method retrieves all IDs of available {@link OntologyManagerFactory} implementations installed in
	 * the OSGi framework
	 * 
	 * @return Arraylist
	 */
	public static ArrayList<String> getOntManagerImplIDs() {
		// test preamble
		if (isDirectAccessTest()) {
			ArrayList<String> repImplIdList = new ArrayList<String>();
			repImplIdList.add(testOntManagerFactoryCls.getName());
			return repImplIdList;
		} else {
			// real use section
			return getServletExtensionsIDForType(OntologyManagerFactory.class);
		}

	}

	/**
	 * Funzione per avere una particolare implemementazione delle API per gestire il repository
	 * 
	 * @param idRepImpl
	 *            id dell'implementazione desiderata
	 * @return handler dell'implementazione desiderata
	 * @throws UnavailableResourceException
	 */
	@SuppressWarnings("unchecked")
	public static OntologyManagerFactory<ModelConfiguration> getOntManagerImpl(String idRepImpl)
			throws UnavailableResourceException {

		OntologyManagerFactory<ModelConfiguration> ontMgrFactory = null;

		// test preamble
		if (isDirectAccessTest()) {
			try {
				ontMgrFactory = testOntManagerFactoryCls.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace(); // this will be caught by the test unit
			} catch (IllegalAccessException e) {
				e.printStackTrace(); // this will be caught by the test unit
			}
		} else
			// real use section
			ontMgrFactory = getServletExtensionByID(idRepImpl, OntologyManagerFactory.class);

		if (ontMgrFactory == null)
			throw new UnavailableResourceException("OntManagerFactory: " + idRepImpl
					+ " is not available among the registered OSGi Ont Manager factories");

		return ontMgrFactory;
	}

	/**
	 * Funzione per avere il numero di implementazioni delle API per gestire il repository
	 * 
	 * @return numero di implementazioni
	 */
	public static int getNumOntManagers() {
		ServiceTracker m_tracker = null;
		int num = 0;

		m_tracker = new ServiceTracker(m_felix, OntologyManagerFactory.class.getName(),
				null);
		m_tracker.open();

		Object[] services = m_tracker.getServices();
		num = services.length;
		m_tracker.close();

		return num;
	}

	@SuppressWarnings("unchecked")
	private static <T extends STOSGIExtension> T getServletExtensionByID(String idRepImpl, Class<T> type) {
		ServiceTracker m_tracker = null;
		T repImpl = null;

		m_tracker = new ServiceTracker(m_felix, type.getName(), null);
		m_tracker.open();

		Object[] services = m_tracker.getServices();
		for (int i = 0; (services != null) && i < services.length; ++i) {
			if (((T) services[i]).getId().equals(idRepImpl)) {
				repImpl = (T) services[i];
				break;
			}
		}
		m_tracker.close();
		return repImpl;
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

}
