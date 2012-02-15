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
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2010.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

package it.uniroma2.art.semanticturkey.utilities;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.utilities.RDFRenderer;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFTypesEnum;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

/**
 * 
 * @author Armando Stellato <a href="mailto:stellato@info.uniroma2.it">stellato@info.uniroma2.it</a>
 * 
 */
public class RDFXMLHelp {

	/**
	 * This method has nothing to do with the RDFXML standard for serializing RDF graphs.<br/>
	 * 
	 * It simply renders the description of a RDF node under a single XML Element, and additional information
	 * that needs to be presented can be defined in the request. <br/>
	 * First, the nature ({@link RDFTypesEnum}) of the node is revealed, among:
	 * <ul>
	 * <li>uri</li>
	 * <li>bnode</li>
	 * <li>plainLiteral</li>
	 * <li>typedLiteral</li>
	 * </ul>
	 * then additional information, such as the node <em>role</em> ({@link RDFResourceRolesEnum}), can be
	 * optionally added<br/>
	 * an optional <code>rendering</code> argument enables for a rendered visualization of the node
	 * 
	 * 
	 * @param parent
	 *            the XML parent element the newly created description is attached to
	 * @param model
	 *            the model to be queried for describing the RDFNode
	 * @param node
	 *            the node which is being described
	 * @param role
	 *            when <code>true</code>, if the node is a resource, it tells the role of the resource (cls,
	 *            ontology, property...). see {@link RDFResourceRolesEnum}
	 * @param rendering
	 *            If true, it provides a human readable representation of the node
	 *            <ul>
	 *            <li>a <code>show</code> attribute representing the node</li>
	 *            <li>if the value is a typed literal, a <code>typeQName</code> attribute providing a qname
	 *            for the datatype</li>
	 *            </ul>
	 * @return
	 * @throws DOMException
	 * @throws ModelAccessException
	 */
	public static Element addRDFNodeXMLElement(Element parent, RDFModel model, ARTNode node, boolean role,
			boolean rendering) throws DOMException, ModelAccessException {
		Element nodeElement;
		if (node.isResource()) {
			if (node.isURIResource()) {
				nodeElement = XMLHelp.newElement(parent, RDFTypesEnum.uri.toString());
				String uri = node.asURIResource().getURI();
				nodeElement.setTextContent(uri);
			} else { // (node.isBlank())
				nodeElement = XMLHelp.newElement(parent, RDFTypesEnum.bnode.toString());
				nodeElement.setTextContent(node.asBNode().getID());
			}
			if (role)
				nodeElement.setAttribute("role", ModelUtilities.getResourceRole(node.asResource(), model)
						.toString());
		} else {
			// literal
			ARTLiteral lit = node.asLiteral();
			ARTURIResource dt = lit.getDatatype();
			if (dt != null) {
				// typed literal
				nodeElement = XMLHelp.newElement(parent, RDFTypesEnum.typedLiteral.toString());
				nodeElement.setAttribute("type", dt.getURI());
				if (rendering)
					nodeElement.setAttribute("typeQName", model.getQName(dt.getURI()));
			} else {
				// plain literal
				nodeElement = XMLHelp.newElement(parent, RDFTypesEnum.plainLiteral.toString());
				String lang = lit.getLanguage();
				if (lang != null)
					nodeElement.setAttribute("lang", lang);
			}
			nodeElement.setTextContent(lit.getLabel());
		}

		if (rendering) {
			nodeElement.setAttribute("show", RDFRenderer.renderRDFNode(model, node));
		}

		return nodeElement;
	}

}
