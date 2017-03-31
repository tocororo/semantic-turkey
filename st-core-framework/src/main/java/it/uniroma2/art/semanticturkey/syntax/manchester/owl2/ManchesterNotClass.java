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

import java.util.Map;

public class ManchesterNotClass extends ManchesterClassInterface{

	private ManchesterClassInterface notClass;
	
	public ManchesterNotClass(ManchesterClassInterface notClass) {
		super(PossType.NOT);
		this.notClass = notClass;
	}
	
	public ManchesterClassInterface getNotClass(){
		return notClass;
	}
	
	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n"+tab+getType());
		sb.append("\n"+tab+"\t"+notClass.print(tab+"\t"));
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {
		if(useUppercaseSyntax){
			return "NOT "+notClass.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
		} else {
			return "not "+notClass.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
		}
	}
	
	
}
