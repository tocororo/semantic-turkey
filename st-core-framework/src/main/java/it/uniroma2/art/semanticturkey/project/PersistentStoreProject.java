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
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;

public class PersistentStoreProject extends Project {

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
		if (supportOntManager != null) {
			supportOntManager.startOntModel("http://example.org/history", null, null);
		}
		newOntManager.startOntModel(getBaseURI(), null, null);

		try (RepositoryConnection conn = newOntManager.getRepository().getConnection()) {
			// conn.begin();

			ValueFactory vf = conn.getValueFactory();

			Set<Resource> contexts = QueryResults.asSet(conn.getContextIDs());

			IRI rdfBaseURI = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns");
			IRI rdfsBaseURI = vf.createIRI("http://www.w3.org/2000/01/rdf-schema");
			IRI owlBaseURI = vf.createIRI("http://www.w3.org/2002/07/owl");
			IRI skosBaseURI = vf.createIRI("http://www.w3.org/2004/02/skos/core");
			IRI skosxlBaseURI = vf.createIRI("http://www.w3.org/2008/05/skos-xl");

			try {
				if (!contexts.contains(rdfBaseURI)) {
					
					logger.debug("Loading RDF vocabulary...");
					conn.add(OntologyManager.class.getResource("rdf.rdf"), rdfBaseURI.stringValue(),
							RDFFormat.RDFXML, rdfBaseURI);
				}

				if (!contexts.contains(rdfsBaseURI)) {
					logger.debug("Loading RDFS vocabulary...");
					conn.add(OntologyManager.class.getResource("rdf-schema.rdf"), rdfsBaseURI.stringValue(),
							RDFFormat.RDFXML, rdfsBaseURI);
				}

				if (!contexts.contains(owlBaseURI)) {
					logger.debug("Loading OWL vocabulary...");
					conn.add(OntologyManager.class.getResource("owl.rdf"), owlBaseURI.stringValue(),
							RDFFormat.RDFXML, owlBaseURI);
				}

				boolean isSKOSXL = Objects.equals(getLexicalizationModel(), SKOSXL_LEXICALIZATION_MODEL);
				boolean isSKOS = isSKOSXL
						|| Objects.equals(getLexicalizationModel(), SKOS_LEXICALIZATION_MODEL)
						|| Objects.equals(getModel(), SKOS_MODEL);

				if (isSKOS && !contexts.contains(skosBaseURI)) {
					logger.debug("Loading SKOS vocabulary...");
					conn.add(OntologyManager.class.getResource("skos.rdf"), skosBaseURI.stringValue(),
							RDFFormat.RDFXML, skosBaseURI);
				}

				if (isSKOSXL && !contexts.contains(skosxlBaseURI)) {
					logger.debug("Loading SKOS-XL vocabulary...");
					conn.add(OntologyManager.class.getResource("skos-xl.rdf"), skosxlBaseURI.stringValue(),
							RDFFormat.RDFXML, skosxlBaseURI);
					conn.setNamespace("skosxl", SKOSXL.NAMESPACE);
				}
			} catch (RepositoryException | IOException e) {
				throw new ModelCreationException(e);
			}

			logger.debug("About to commit the loaded triples");
			// conn.commit();
			logger.debug("Triples loaded");
		}
	}

}
