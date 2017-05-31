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
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappingUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;

public class SaveToStoreProject extends Project {

	protected final static String triplesFileName = "triples.nt";
	File triplesFile;

	SaveToStoreProject(String projectName, File projectDir) throws ProjectCreationException {
		super(projectName, projectDir);
		try {
			triplesFile = new File(projectDir, triplesFileName);
			if (!triplesFile.exists()) {
				if (!triplesFile.createNewFile())
					throw new ProjectCreationException(
							"unable to create triple file inside project: " + projectName);
			}
			nsPrefixMappingsPersistence = new NSPrefixMappings(projectDir, false);
		} catch (IOException e) {
			throw new ProjectCreationException(e);
		}
	}

	public void save() throws ProjectUpdateException {
		try {
			RDFModel model = getOntModel();
			try (RepositoryConnection conn = getRepository().getConnection()) {
				conn.export(Rio.createWriter(RDFFormat.NTRIPLES, new FileOutputStream(triplesFile)),
						conn.getValueFactory().createIRI(getNewOntologyManager().getBaseURI()));
			}
			nsPrefixMappingsPersistence.updatePrefixMappingRegistry();
		} catch (FileNotFoundException e) {
			throw new ProjectUpdateException("unable to write to the project file");
		} catch (NSPrefixMappingUpdateException e) {
			throw new ProjectUpdateException(e);
		}
	}

	protected void loadTriples() throws ModelCreationException {
		try {
			logger.debug("clearing RDF data");
			newOntManager.clearData();
			logger.debug("starting the ont model");
			newOntManager.startOntModel(getBaseURI(), null, null);
			logger.debug("loading ontology data");
			newOntManager.loadOntologyData(triplesFile, getBaseURI(), RDFFormat.NTRIPLES,
					SimpleValueFactory.getInstance().createIRI(getBaseURI()),
					TransitiveImportMethodAllowance.web, new HashSet<>());
		} catch (Exception e) {
			throw new ModelCreationException(e);
		}
	}

}
