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
  * The Original Code is SemanticTurkey.
  *
  * The Initial Developer of the Original Code is University of Roma Tor Vergata.
  * Portions created by University of Roma Tor Vergata are Copyright (C) 2014.
  * All Rights Reserved.
  *
  * SemanticTurkey was developed by the Artificial Intelligence Research Group
  * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
  * Current information about SemanticTurkey can be obtained at 
  * http://semanticturkey.uniroma2.it
  *
  */
package it.uniroma2.art.semanticturkey.resources;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;

import com.google.common.base.Objects;

/**
 * Metadata describing a dataset.
 * 
 */
public class DatasetMetadata {
	private String baseURI;
	private String voidDocument;
	private String sparqlEndpoint;
	private boolean dereferenceable;
	private RenderingEngine renderingEngine;
	private ARTURIResource lexicalModel;
	

	public DatasetMetadata(String baseURI, String voidDocument, String sparqlEndpoint,
			boolean dereferenceable, RenderingEngine renderingEngine, ARTURIResource lexicalModel) {
		this.baseURI = baseURI;
		this.voidDocument = voidDocument;
		this.sparqlEndpoint = sparqlEndpoint;
		this.dereferenceable = dereferenceable;
		this.renderingEngine = renderingEngine;
		this.lexicalModel = lexicalModel;
	}

	public String getSparqlEndpoint() {
		return sparqlEndpoint;
	}

	public void setSparqlEndpoint(String sparqlEndpoint) {
		this.sparqlEndpoint = sparqlEndpoint;
	}

	public boolean isDereferenceable() {
		return dereferenceable;
	}

	public void setDereferenceable(boolean dereferenceable) {
		this.dereferenceable = dereferenceable;
	}

	public String getBaseURI() {
		return baseURI;
	}
	
	public boolean isAccessible() {
		return getSparqlEndpoint() != null || isDereferenceable();
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("baseURI", this.baseURI).toString(); 
	}
	
}
