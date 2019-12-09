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

import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ManchesterClassInterface;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;


public class ManchesterValueClass extends ManchesterClassInterface {

	private boolean inverse = false;
	private IRI prop;
	private Value value;

	public ManchesterValueClass(IRI prop, Value value) {
		super(PossType.VALUE);
		this.prop = prop;
		this.value = value;
	}
	
	public ManchesterValueClass(boolean inverse, IRI prop, Value value) {
		super(PossType.VALUE);
		this.inverse = inverse;
		this.prop = prop;
		this.value = value;
	}
	
	public boolean hasInverse(){
		return inverse;
	}

	public IRI getProp() {
		return prop;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n" + tab + getType());
		if(inverse){
			sb.append("\n" + tab + "\t inverse");
		}
		sb.append("\n" + tab + "\t" + prop.stringValue());
		String valueAsString="";
		if(value instanceof IRI){
			valueAsString =((IRI)value).stringValue();
		} else if(value instanceof Literal){
			valueAsString = ((Literal)value).stringValue();
		}
		sb.append("\n" + tab + "\t" + valueAsString);
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax){
		String valueString;
		
		String inverseOrEmpty="";
		if(inverse){
			inverseOrEmpty="inverse ";
		}
		
		if (value instanceof IRI) {
			valueString = printRes(getPrefixName, namespaceToPrefixsMap, (IRI)value);
		} else if (value instanceof Literal) {
			valueString = printLiteral(getPrefixName, namespaceToPrefixsMap, (Literal) value);
		} else {
			// this should never happen
			valueString = "";
		}
		if(useUppercaseSyntax){
			return inverseOrEmpty.toUpperCase() + printRes(getPrefixName, namespaceToPrefixsMap, prop) + " VALUE " + valueString;
		} else {
			return inverseOrEmpty + printRes(getPrefixName, namespaceToPrefixsMap, prop) + " value " + valueString;
		}
	}
}
