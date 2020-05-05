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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;

public class ManchesterLiteralListClass extends ManchesterClassInterface {

	private List<Literal> literalList;

	public ManchesterLiteralListClass(List<Literal> literalList) {
		super(PossType.LITERALLIST);
		if (literalList != null) {
			this.literalList = literalList;
		} else {
			this.literalList = new ArrayList<>();
		}
	}

	public ManchesterLiteralListClass() {
		super(PossType.LITERALLIST);
		this.literalList = new ArrayList<Literal>();
	}

	public void addOneOf(Literal literal) {
		literalList.add(literal);
	}

	public List<Literal> getLiteralList() {
		return literalList;
	}

	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n" + tab + getType());
		for (int i = 0; i < literalList.size(); ++i) {
			sb.append("\n" + tab + "\t" + printLiteral(false, new HashMap<String, String>(), literalList.get(i)));
		}
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {
		String manchExpr = "{";
		boolean first = true;
		for (Literal literal : literalList) {
			if (!first) {
				manchExpr += ", ";
			}
			first = false;
			manchExpr += printLiteral(getPrefixName, namespaceToPrefixsMap, literal);
		}
		manchExpr += "}";
		return manchExpr;
	}

}
