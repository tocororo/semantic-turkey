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
  * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
  * All Rights Reserved.
  *
  * The ART Ontology API were developed by the Artificial Intelligence Research Group
  * (art.uniroma2.it) at the University of Roma Tor Vergata
  * Current information about the ART Ontology API can be obtained at 
  * http//art.uniroma2.it/owlart
  *
  */

package it.uniroma2.art.semanticturkey.exceptions.manchester;

 import java.util.List;

 /**
 * @author Andrea Turbati
 *
 */
public class ManchesterParserException extends Exception {

     private String msg;
     private int pos;
     private String offendingTerm;
     private List<String> expectedTokenList;

     /**
	 * 
	 */
	private static final long serialVersionUID = 4759454918851129781L;

     public ManchesterParserException(String msg, int pos, String offendingTerm, List<String> expectedTokenList) {
         super(msg);
         this.msg = msg;
         this.pos = pos;
         this.offendingTerm = offendingTerm;
         this.expectedTokenList = expectedTokenList;
     }

     public String getMsg() {
         return msg;
     }

     public int getPos() {
         return pos;
     }

     public String getOffendingTerm() {
         return offendingTerm;
     }

     public List<String> getExpectedTokenList() {
         return expectedTokenList;
     }
}
