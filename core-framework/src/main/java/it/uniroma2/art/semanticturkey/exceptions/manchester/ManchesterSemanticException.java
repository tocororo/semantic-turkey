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
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.exceptions.manchester;


import it.uniroma2.art.semanticturkey.exceptions.IncompatibleResourceException;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

/**
 * @author Andrea Turbati
 *
 */
public class ManchesterSemanticException extends IncompatibleResourceException {

	private String msg;
	private IRI resource;
	private String qname;
	private int pos;

	/**
	 *
	 */
	private static final long serialVersionUID = 6319220572009520791L;


	public ManchesterSemanticException(String msg, int pos, String qname, IRI resource) {
		super(msg);
		this.msg = msg;
		this.pos = pos;
		this.qname = qname;
		this.resource = resource;
	}

	public String getMsg() {
		return msg;
	}

	public int getPos() {
		return pos;
	}

	public IRI getResource() {
		return resource;
	}

	public String getQname() {
		return qname;
	}
}
