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

package it.uniroma2.art.semanticturkey;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * <p>
 * Main class in charge of:
 * </p>
 * <ul>
 * <li>initializing the application</li>
 * <li>activating the loggers and point them to the logging files</li>
 * <li>creating the web server and initialize the servlet</li>
 * <li>loading the Ontology Manager OSGi bundles</li>
 * </ul>
 * 
 * @author Armando Stellato <a href="mailto:stellato@info.uniroma2.it">stellato@info.uniroma2.it</a><br/>
 *         Contributor: Andrea Turbati <a href="mailto:turbati@info.uniroma2.it">turbati@info.uniroma2.it</a>
 * 
 */
public class SemanticTurkey implements BundleActivator {

	protected static Logger logger = LoggerFactory.getLogger(SemanticTurkey.class);

	/**
	 * main initialization method which is invoked by the Semantic Turkey javascript client through
	 * Java/JavaScript XPCOM Bridge
	 * 
	 * @param extensionPath
	 *            path to the installed Semantic Turkey extension
	 * @return
	 */
	public static String initialize(String extensionPath) {
		// TODO pass also the port to the initialize method, so that user
		// preferences can modify the
		// server port

		// URL log4jPropertiesFileURL =
		// SemanticTurkey.class.getResource("log4j.properties");
		// PropertyConfigurator.configure(log4jPropertiesFileURL);
		File extensionDir = null;
		try {
			extensionDir = SemanticTurkeyOperations.uriToFile(extensionPath);
		} catch (URISyntaxException e) {
			// TODO catch this error and do something in the client which
			// reports the error
			e.printStackTrace();
		}

		logger.debug("initialize...");

		try {
			Resources.initializeUserResources(extensionDir.getAbsolutePath());
			XMLHelp.initialize();
		} catch (Exception e) {
			// TODO catch this error and do something in the client which
			// reports the error
			e.printStackTrace();
		}

		logger.debug("userDataPath: " + extensionDir.getAbsolutePath());

		// Inizialize the part related to the proxy (usefull only if the proxy's parameters are passed to the
		// JVM)
		initializeProxyAuthenticatorHTTP();

		// createWebServer();
		// PluginManager.loadOntManagersImpl();
		// // SemanticTurkey.class.getResource("log4j.properties").toString();
		return null;
	}

	private static void initializeProxyAuthenticatorHTTP() {
		final String proxyUser = System.getProperty("http.proxyUser");
		final String proxyPassword = System.getProperty("http.proxyPassword");

		if (proxyUser != null && proxyPassword != null) {
			Authenticator.setDefault(new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
				}
			});
		}
	}

	public void start(BundleContext arg0) throws Exception {
		String extensionDir = new File(System.getProperty("user.dir")).toURI().toString();
		logger.info("ST Home Directory: " + extensionDir);
		System.out.println("ST Home Directory: " + extensionDir);
		// sets the context class loader so that e.g. we can use the registered service providers under META-INF/services
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(SemanticTurkey.class.getClassLoader());
		try {
			initialize(extensionDir);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
		logger.info("ST Starting ...");
		System.out.println("ST Starting ...");
	}

	public void stop(BundleContext arg0) throws Exception {
		// TODO Auto-generated method stub
	}

}
