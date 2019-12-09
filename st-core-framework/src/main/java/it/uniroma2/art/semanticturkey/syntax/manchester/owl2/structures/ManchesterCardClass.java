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

public class ManchesterCardClass extends ManchesterClassInterface{

	private boolean inverse = false;
	private int card;
	private IRI prop;
	private ManchesterClassInterface classCard;
	
	public ManchesterCardClass(PossType type, int card, IRI prop) {
		super(type);
		this.card = card;
		this.prop = prop;
		this.classCard = null;
	}
	
	public ManchesterCardClass(PossType type, int card, IRI prop, ManchesterClassInterface classCard) {
		super(type);
		this.card = card;
		this.prop = prop;
		this.classCard = classCard;
	}
	
	public ManchesterCardClass(boolean inverse, PossType type, int card, IRI prop) {
		super(type);
		this.inverse = inverse;
		this.card = card;
		this.prop = prop;
		this.classCard = null;
	}
	
	public ManchesterCardClass(boolean inverse, PossType type, int card, IRI prop, ManchesterClassInterface classCard) {
		super(type);
		this.inverse = inverse;
		this.card = card;
		this.prop = prop;
		this.classCard = classCard;
	}

	public boolean hasInverse(){
		return inverse;
	}
	
	public int getCard() {
		return card;
	}

	public IRI getProp() {
		return prop;
	}
	
	public ManchesterClassInterface getClassCard(){
		return classCard;
	}
	
	@Override
	public String print(String tab) {
		StringBuffer sb = new StringBuffer();
		sb.append("\n"+tab+getType());
		if(inverse){
			sb.append("\n" + tab + "\t inverse");
		}
		sb.append("\n"+tab+"\t"+prop.stringValue());
		sb.append("\n"+tab+"\t"+card);
		if(classCard!=null){
			sb.append(classCard.print("\t"+tab));
		}
		return sb.toString();
	}

	@Override
	public String getManchExpr(Map<String, String> namespaceToPrefixsMap, boolean getPrefixName, 
			boolean useUppercaseSyntax) {
		String inverseOrEmpty="";
		if(inverse){
			if(useUppercaseSyntax){
				inverseOrEmpty="INVERSE ";
			} else{
				inverseOrEmpty="inverse ";
			}
		}
		String manchExpr = inverseOrEmpty+printRes(getPrefixName, namespaceToPrefixsMap, prop);
		if(getType() == PossType.MIN){
			if(useUppercaseSyntax){
				manchExpr += " MIN ";
			} else{
				manchExpr += " min ";
			}
		} else if(getType() == PossType.MAX){
			if(useUppercaseSyntax){
				manchExpr += " MAX ";
			} else{
				manchExpr += " max ";
			}
		} else{ // is is exactly
			if(useUppercaseSyntax){
				manchExpr += " EXACTLY ";
			} else{
				manchExpr += " exaclty ";
			}
		}
		manchExpr += card;
		
		if(classCard != null){
			manchExpr += " "+classCard.getManchExpr(namespaceToPrefixsMap, getPrefixName, useUppercaseSyntax);
		}
		
		return manchExpr;
	}

}
