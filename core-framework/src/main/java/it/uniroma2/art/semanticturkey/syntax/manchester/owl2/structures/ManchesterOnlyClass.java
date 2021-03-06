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

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

public class ManchesterOnlyClass extends ManchesterClassInterface {

	private boolean inverse = false;
	private IRI onlyProp;
	private ManchesterClassInterface onlyClass;
	
	
	public ManchesterOnlyClass(IRI onlyProp, ManchesterClassInterface onlyClass) {
		super(PossType.ONLY);
		this.onlyClass = onlyClass;
		this.onlyProp = onlyProp;
	}
	
	public ManchesterOnlyClass(boolean inverse, IRI onlyProp, ManchesterClassInterface onlyClass) {
		super(PossType.ONLY);
		this.inverse = inverse;
		this.onlyProp = onlyProp;
		this.onlyClass = onlyClass;
	}
	
	public boolean hasInverse(){
		return inverse;
	}

	public IRI getOnlyProp() {
		return onlyProp;
	}

	public ManchesterClassInterface getOnlyClass() {
		return onlyClass;
	}

	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n" + tab + getType());
		if(inverse){
			sb.append("\n" + tab + "\t inverse");
		}
		sb.append("\n" + tab + "\t" + onlyProp.stringValue());
		sb.append(onlyClass.print(tab + "\t"));
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {
		String inverseOrEmpty="";
		if(inverse){
			inverseOrEmpty="inverse ";
		}
		if(useUppercaseSyntax){
			return inverseOrEmpty.toUpperCase() + printRes(getPrefixName, namespaceToPrefixsMap, onlyProp) + " ONLY "
				+ onlyClass.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
		} else {
			return inverseOrEmpty+ printRes(getPrefixName, namespaceToPrefixsMap, onlyProp) + " only "
					+ onlyClass.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
		}
	}
}
