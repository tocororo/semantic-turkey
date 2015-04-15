 /*
  * The contents of this file are subject to the Mozilla Public License
  * Version 1.1 (the "License");  you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
  * the specific language governing rights and limitations under the License.
  *
  * The Original Code is Linguistic Watermark.
  *
  * The Initial Developer of the Original Code is University of Roma Tor Vergata.
  * Portions created by University of Roma Tor Vergata are Copyright (C) 2005.
  * All Rights Reserved.
  *
  * Linguistic Watermark was developed by the Artificial Intelligence Research Group
  * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
  * Current information about Linguistic Watermark can be obtained at 
  * http://art.uniroma2.it/software/LinguisticWatermark/
  *
  */

package it.uniroma2.art.semanticturkey.plugin.configuration;

public class ConfParameterNotFoundException extends Exception{
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 5458230462908450196L;

	public ConfParameterNotFoundException(String msg) {
        super(msg);
    }
    
    public ConfParameterNotFoundException(Exception e) {
        super(e);
    }
}
