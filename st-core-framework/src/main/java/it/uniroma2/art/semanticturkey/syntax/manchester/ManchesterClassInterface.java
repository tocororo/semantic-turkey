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

package it.uniroma2.art.semanticturkey.syntax.manchester;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

public abstract class ManchesterClassInterface {
	private PossType type;

	public static enum PossType {
		BASE, AND, OR, NOT, ONEOF, SOME, ONLY, MIN, MAX, EXACTLY, VALUE
	}

	public ManchesterClassInterface(PossType type) {
		this.type = type;
	}

	public PossType getType() {
		return type;
	}

	public String printRes(boolean getPrefixName, Map<String, String> namespaceToPrefixsMap, IRI res) {
		if(!getPrefixName){
			return "<"+res.stringValue()+">";
		}
		
		String prefix = namespaceToPrefixsMap.get(res.getNamespace());
		
		if(prefix == null) {
			return "<" + res.stringValue() + ">";
		} else{
			return prefix+":"+res.getLocalName();
		}
	}

	/**
	 * Returns the representation of this class expression conforming to the Manchester syntax. The parameter
	 * <code>getPrefixName</code> controls whether URIs are shortened into qualified names or presented in
	 * their full form. The qualified names use prefixes defined by the model that has been used to construct
	 * this object, unless the parameter <code>prefixMapping</code> holds a non-null reference to an
	 * alternative {@link PrefixMapping}.
	 * 
	 * @param prefixMapping
	 * @param getPrefixName
	 * @return
	 * @throws ModelAccessException
	 */
	public abstract String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax);

	/**
	 * A shortcut for {@link #getManchExpr(PrefixMapping, boolean)} with the first parameter set to
	 * <code>null</code>
	 * 
	 * @param getPrefixName
	 * @return
	 * @throws ModelAccessException
	 */
	public String getManchExpr(boolean useUppercaseSyntax){
		return getManchExpr(null, false, useUppercaseSyntax);
	}

	public abstract String print(String tab);
}
