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
package it.uniroma2.art.semanticturkey.vocabulary;

/**
 * this enum class provides a closed list of RDF node types. Can be used as a fast-to-use reference vocabulary
 * for managing different behavior of methods according to type of processed resources
 * 
 * @author Armando Stellato
 * 
 */
public enum RDFTypesEnum {

	undetermined, resource, bnode, uri, literal;

	public static boolean isLiteral(RDFTypesEnum lit) {
		return (lit == RDFTypesEnum.literal);
	}
	
	public static boolean isResource(RDFTypesEnum type) {
		return (type == RDFTypesEnum.uri
		|| type == RDFTypesEnum.bnode
		|| type == RDFTypesEnum.resource);
	}
	
}
