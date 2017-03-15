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
import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.semanticturkey.changetracking.sail.RepositoryRegistry;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;

public class PersistentStoreProject<MODELTYPE extends RDFModel> extends Project<MODELTYPE> {

	PersistentStoreProject(String projectName, File projectDir) throws ProjectCreationException {
		super(projectName, projectDir);
		try {
			nsPrefixMappingsPersistence = new NSPrefixMappings(projectDir, true);
		} catch (IOException e) {
			throw new ProjectCreationException(e);
		}
	}

	@Override
	protected void loadTriples() throws ModelCreationException {
		logger.debug("loading triples");
		if (ontManager != null) {
			ontManager.startOntModel(getBaseURI(), getProjectStoreDir(), modelConfiguration);
		} else {
			supportOntManager.startOntModel("http://example.org/history", null, null);
			newOntManager.startOntModel(getBaseURI(), null, null);

			try (RepositoryConnection conn = newOntManager.getRepository().getConnection()) {
				ValueFactory vf = conn.getValueFactory();

				IRI rdfBaseURI = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns");
				IRI rdfsBaseURI = vf.createIRI("http://www.w3.org/2000/01/rdf-schema");
				IRI owlBaseURI = vf.createIRI("http://www.w3.org/2002/07/owl");
				IRI skosBaseURI = vf.createIRI("http://www.w3.org/2004/02/skos/core");
				IRI skosxlBaseURI = vf.createIRI("http://www.w3.org/2008/05/skos-xl");

				try {
					if (!conn.hasStatement(null, null, null, false, rdfBaseURI)) {
						conn.add(OntologyManager.class.getResourceAsStream("rdf.rdf"),
								rdfBaseURI.stringValue(), RDFFormat.RDFXML, rdfBaseURI);
					}
					
					if (!conn.hasStatement(null, null, null, false, rdfsBaseURI)) {
						conn.add(OntologyManager.class.getResourceAsStream("rdf-schema.rdf"),
								rdfsBaseURI.stringValue(), RDFFormat.RDFXML, rdfsBaseURI);
					}
					
					if (!conn.hasStatement(null, null, null, false, owlBaseURI)) {
						conn.add(OntologyManager.class.getResourceAsStream("owl.rdf"),
								owlBaseURI.stringValue(), RDFFormat.RDFXML, owlBaseURI);
					}
					
					if (SKOSModel.class.isAssignableFrom(getModelType())) {
						conn.add(OntologyManager.class.getResourceAsStream("skos.rdf"),
								skosBaseURI.stringValue(), RDFFormat.RDFXML, skosBaseURI);
					}
					
					if (SKOSXLModel.class.isAssignableFrom(getModelType())) {
						conn.add(OntologyManager.class.getResourceAsStream("skos-xl.rdf"),
								skosxlBaseURI.stringValue(), RDFFormat.RDFXML, skosxlBaseURI);
					}
				} catch (RepositoryException | IOException e) {
					throw new ModelCreationException(e);
				} catch (ProjectInconsistentException e) {
					throw new ModelCreationException(e);
				}
			}
		}
	}

}
