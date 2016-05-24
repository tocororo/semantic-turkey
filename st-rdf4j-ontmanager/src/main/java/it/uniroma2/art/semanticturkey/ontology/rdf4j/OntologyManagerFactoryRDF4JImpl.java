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
 * The Original Code is ST OntologyManager - RDF4J Implementation.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2008.
 * All Rights Reserved.
 *
 * The ART Ontology API - RDF4J Implementation were developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ST OntologyManager - RDF4J Implementation can be obtained at 
 * http//semanticturkey.uniroma2.it
 *
 */

package it.uniroma2.art.semanticturkey.ontology.rdf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.UnloadableModelConfigurationException;
import it.uniroma2.art.owlart.models.UnsupportedModelConfigurationException;
import it.uniroma2.art.owlart.rdf4jimpl.factory.ARTModelFactoryRDF4JImpl;
import it.uniroma2.art.owlart.rdf4jimpl.models.conf.RDF4JModelConfiguration;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerFactoryImpl;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;
import it.uniroma2.art.semanticturkey.project.Project;

public class OntologyManagerFactoryRDF4JImpl extends OntologyManagerFactoryImpl<RDF4JModelConfiguration> {

	protected static Logger logger = LoggerFactory.getLogger(OntologyManagerFactoryRDF4JImpl.class);
	
	public ModelFactory<RDF4JModelConfiguration> createModelFactory() {
		return new ARTModelFactoryRDF4JImpl();
	}

	public <MODELTYPE extends RDFModel> STOntologyManager<MODELTYPE> createOntologyManager(
			Project<MODELTYPE> project) {
		return new STOntologyManagerRDF4JImpl<MODELTYPE>(project);
	}

	@SuppressWarnings("unchecked")
	public RDF4JModelConfiguration createModelConfigurationObject(String mcTypeString)
			throws UnsupportedModelConfigurationException, UnloadableModelConfigurationException,
			ClassNotFoundException {

		Class<? extends RDF4JModelConfiguration> mcType = (Class<? extends RDF4JModelConfiguration>) Class.forName(mcTypeString);

		logger.debug("class loader of static: " + ARTModelFactoryRDF4JImpl.class.getSimpleName() + " : " + ARTModelFactoryRDF4JImpl.class.getClassLoader());
		logger.debug("class loader of static: " + OntologyManagerFactoryRDF4JImpl.class.getSimpleName() + " : " + OntologyManagerFactoryRDF4JImpl.class.getClassLoader());
		logger.debug("class loader of: " + this.getClass().getSimpleName() + " instance: " + this.getClass().getClassLoader());
		logger.debug("class loader of resolved model configuration class: " + mcType.getClass().getSimpleName() + "inside: " + this.getClass().getSimpleName() + " = " + mcType.getClassLoader());
		
		return createModelFactory().createModelConfigurationObject(mcType);
	}

}
