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

package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

public class ManchesterOneOfClass extends ManchesterClassInterface {

	private List<IRI> oneOfList;

	public ManchesterOneOfClass(List<IRI> oneOfList) {
		super(PossType.ONEOF);
		if (oneOfList != null) {
			this.oneOfList = oneOfList;
		} else {
			this.oneOfList = new ArrayList<>();
		}
	}

	public ManchesterOneOfClass() {
		super(PossType.ONEOF);
		this.oneOfList = new ArrayList<IRI>();
	}

	public void addOneOf(IRI oneOf) {
		oneOfList.add(oneOf);
	}

	public List<IRI> getOneOfList() {
		return oneOfList;
	}

	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n" + tab + getType());
		for (int i = 0; i < oneOfList.size(); ++i) {
			sb.append("\n" + tab + "\t" + oneOfList.get(i).stringValue());
		}
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {
		String manchExpr = "{";
		boolean first = true;
		for (IRI oneOf : oneOfList) {
			if (!first) {
				manchExpr += ", ";
			}
			first = false;
			manchExpr += printRes(getPrefixName, namespaceToPrefixsMap, oneOf);
		}
		manchExpr += "}";
		return manchExpr;
	}

}
