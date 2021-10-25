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
 * The Original Code is ART OWL API.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * The ART OWL API were developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about the ART OWL API can be obtained at
 * http://art.uniroma2.it/owlart
 *
 */

package it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import java.util.Map;

public abstract class ManchesterClassInterface {
	private PossType type;

	public static enum PossType {
		BASE, AND, OR, NOT, ONEOF, SOME, ONLY, MIN, MAX, EXACTLY, VALUE, SELF, LITERALLIST, DATATYPERESTRICTION,
		DATARANGE, DATACONJUCTION
	}

	public ManchesterClassInterface(PossType type) {
		this.type = type;
	}

	public PossType getType() {
		return type;
	}

	protected String printRes(boolean getPrefixName, Map<String, String> namespaceToPrefixsMap, IRI res) {
		if (!getPrefixName) {
			return "<" + res.stringValue() + ">";
		}

		if (namespaceToPrefixsMap.containsKey(res.getNamespace())) {
			return namespaceToPrefixsMap.get(res.getNamespace()) + ":" + res.getLocalName();
		} else {
			return printRes(false, null, res);
		}
	}

	protected String printLiteral(boolean getPrefixName, Map<String, String> namespaceToPrefixsMap, Literal literal) {
		/*distinguish two cases:
		- literals representing numbers (xsd:integer , xsd:decimal, xsd:float)
		- other types of literals
		*/
		//if (literal.getDatatype() != null && (literal.getDatatype().equals(XSD.INTEGER) || literal.getDatatype().equals(XSD.DECIMAL)
		//		|| literal.getDatatype().equals(XSD.FLOAT))) {
		if (literal.getDatatype() != null && literal.getDatatype().equals(XSD.INTEGER)) {
			//in case of xsd:integer, do not show the ^^xsd:integer, for all other cases, show the datatype
			return literal.stringValue();
		} else {
			String valueString = "\"" + NTriplesUtil.escapeString(literal.getLabel()) + "\"";
			if (literal.getLanguage().isPresent()) {
				valueString += "@" + literal.getLanguage().get();
			} else if (literal.getDatatype() != null) {
				valueString += "^^" + printRes(getPrefixName, namespaceToPrefixsMap, literal.getDatatype());
			}
			return valueString;
		}
	}

	/**
	 * Returns the representation of this class expression conforming to the Manchester syntax. The parameter
	 * <code>getPrefixName</code> controls whether URIs are shortened into qualified names or presented in
	 * their full form. The qualified names use prefixes defined in the parameter <code>prefixMapping</code>.
	 *
	 * @param namespaceToPrefixsMap the prefix map class to use then getPrefixName is true
	 * @param getPrefixName         to use the qname, if the appropriate prefix has been defined
	 * @param useUppercaseSyntax    to return the the reserved keyword in upper (true) or lower case (false)
	 * @return
	 */
	public abstract String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName,
			boolean useUppercaseSyntax);

	/**
	 * A shortcut for {@link #getManchExpr(Map, boolean, boolean)}  with the first parameter set to
	 * <code>null</code>
	 *
	 * @param useUppercaseSyntax to return the the reserved keyword in upper (true) or lower case (false)
	 * @return
	 */
	public String getManchExpr(boolean useUppercaseSyntax) {
		return getManchExpr(null, false, useUppercaseSyntax);
	}

	public abstract String print(String tab);
}
