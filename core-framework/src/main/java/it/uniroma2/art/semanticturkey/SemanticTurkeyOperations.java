/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

package it.uniroma2.art.semanticturkey;

import java.io.File;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Donato Griesi, Armando Stellato
 * 
 */
public class SemanticTurkeyOperations {
	protected static Logger logger = LoggerFactory.getLogger(SemanticTurkeyOperations.class);

	/**
	 * Funzion che restituisce il File relativo a URI
	 * 
	 * @param uri
	 *            String
	 * @return File
	 */
	static public File uriToFile(String uri) throws URISyntaxException {
		int percentU = uri.indexOf("%u");
		if (percentU >= 0) {
			StringBuffer sb = new StringBuffer(uri.length());

			int start = 0;
			while (percentU > 0) {
				sb.append(uri.substring(start, percentU));

				char c = (char) Integer.parseInt(uri.substring(percentU + 2, percentU + 6), 16);

				sb.append(c);

				start = percentU + 6;
				percentU = uri.indexOf("%u", start);
			}

			sb.append(uri.substring(start));
			uri = sb.toString();
		}
		return new File(new java.net.URI(uri));
	}

}
