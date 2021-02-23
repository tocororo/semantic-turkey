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
 * The Original Code is ART Ontology API.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * ART Ontology API was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ART Ontology API can be obtained at 
 * http//art.uniroma2.it/owlart
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.data.role;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.lime.model.vocabulary.VARTRANS;

/**
 * this enum class provides a closed list of resource types according to the OWL vocabulary. Can be used as a
 * fast-to-use reference vocabulary for managing different behavior of methods according to type of processed
 * resources
 * 
 * @author Armando Stellato
 * @author Manuel Fiorelli
 * 
 */
public enum RDFResourceRole {

	undetermined, cls, individual, property, objectProperty, datatypeProperty, annotationProperty,
	ontologyProperty, ontology, dataRange, concept, conceptScheme, xLabel, skosCollection,
	skosOrderedCollection, limeLexicon, ontolexLexicalEntry, ontolexForm, ontolexLexicalSense, decompComponent,
	vartransTranslationSet;

	static Map<RDFResourceRole, IRI> map;
	static {
		map = new HashMap<RDFResourceRole, IRI>();
		map.put(cls, OWL.CLASS); // todo, WHAT TO DO WITH RDFS CLASS?
		map.put(property, RDF.PROPERTY);
		map.put(objectProperty, OWL.OBJECTPROPERTY);
		map.put(datatypeProperty, OWL.DATATYPEPROPERTY);
		map.put(annotationProperty, OWL.ANNOTATIONPROPERTY);
		map.put(ontologyProperty, OWL.ONTOLOGYPROPERTY);
		map.put(ontology, OWL.ONTOLOGY);
		map.put(dataRange, OWL.DATARANGE);
		map.put(concept, SKOS.CONCEPT);
		map.put(conceptScheme, SKOS.CONCEPT_SCHEME);
		map.put(xLabel, SKOSXL.LABEL);
		map.put(skosCollection, SKOS.COLLECTION);
		map.put(skosOrderedCollection, SKOS.ORDERED_COLLECTION);
		map.put(limeLexicon, LIME.LEXICON);
		map.put(ontolexLexicalEntry, ONTOLEX.LEXICAL_ENTRY);
		map.put(ontolexForm, ONTOLEX.FORM);
		map.put(ontolexLexicalSense, ONTOLEX.LEXICAL_SENSE);
		map.put(decompComponent, DECOMP.COMPONENT);
		map.put(vartransTranslationSet, VARTRANS.TRANSLATION_SET);
	}

	public IRI getIRI() {
		return map.get(this);
	}

	public boolean subsumes(RDFResourceRole role) {
		return subsumes(this, role);
	}

	public boolean isProperty() {
		return isProperty(this);
	}

	public boolean isClass() {
		return isClass(this);
	}

	public boolean isSkosCollection() {
		return isSkosCollection(this);
	}

	public static boolean subsumes(RDFResourceRole subsumer, RDFResourceRole subsumee) {
		return subsumes(subsumer, subsumee, false);
	}

	public static boolean subsumes(RDFResourceRole subsumer, RDFResourceRole subsumee,
			boolean undeterminedSubsumeesAll) {
		if (subsumer == subsumee) {
			return true;
		}

		if (subsumer == RDFResourceRole.undetermined && undeterminedSubsumeesAll) {
			return true;
		}

		if (subsumer == property) {
			return subsumee == objectProperty || subsumee == datatypeProperty
					|| subsumee == annotationProperty || subsumee == RDFResourceRole.ontologyProperty;
		}

		if (subsumer == skosCollection && subsumee == skosOrderedCollection) {
			return true;
		}

		if (subsumer == cls && subsumee == dataRange) {
			return true;
		}

		return false;
	}

	public static boolean isProperty(RDFResourceRole role) {
		return subsumes(property, role);
	}

	public static boolean isClass(RDFResourceRole role) {
		return subsumes(cls, role);
	}

	public static boolean isSkosCollection(RDFResourceRole role) {
		return subsumes(skosCollection, role);
	}

}
