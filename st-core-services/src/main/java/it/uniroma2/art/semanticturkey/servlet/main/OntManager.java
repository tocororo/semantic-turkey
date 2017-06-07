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

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.servlet.main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import it.uniroma2.art.semanticturkey.exceptions.HTTPParameterUnspecifiedException;
import it.uniroma2.art.semanticturkey.plugin.extpts.ServiceAdapter;
import it.uniroma2.art.semanticturkey.resources.MirroredOntologyFile;
import it.uniroma2.art.semanticturkey.resources.OntologiesMirror;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponse;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * @author Armando Stellato
 */
@Component(value="OntManagerOLD")
public class OntManager extends ServiceAdapter {
	protected static Logger logger = LoggerFactory.getLogger(OntManager.class);

	static int webUpdate = 0;
	static int localUpdate = 1;

	// GET REQUESTS
	protected static String getOntologyMirror = "getOntologyMirror";
	protected static String deleteOntMirrorEntry = "deleteOntMirrorEntry";
	protected static String updateOntMirrorEntry = "updateOntMirrorEntry";

	// PARS
	static final public String ontMgrIDField = "ontMgrID";

	@Autowired
	public OntManager(@Value("OntManager") String id) {
		super(id);
	}

	public Logger getLogger() {
		return logger;
	}

	public Response getPreCheckedResponse(String request) throws HTTPParameterUnspecifiedException {
		fireServletEvent();

		if (request.equals(getOntologyMirror)) {
			return getOntologyMirrorTable();
		} else if (request.equals(deleteOntMirrorEntry)) {
			String baseURI = setHttpPar("ns");
			String cacheFileName = setHttpPar("file");
			return deleteOntologyMirrorEntry(baseURI, cacheFileName);
		} else if (request.equals(updateOntMirrorEntry)) {
			String baseURI = setHttpPar("baseURI");
			String mirrorFileName = setHttpPar("mirrorFileName");
			String srcLoc = setHttpPar("srcLoc");
			String location = null;
			int updateType = 0;
			if (srcLoc.equals("wbu")) {
				location = baseURI;
			} else if (srcLoc.equals("walturl")) {
				location = setHttpPar("altURL");
			} else if (srcLoc.equals("lf")) {
				File localFile = setHttpMultipartFilePar("localFile");
				location = localFile.getAbsolutePath();
				updateType = 1;
			} else {
				return ServletUtilities.getService().createExceptionResponse(request,
						"uncorrect or unspecified srcLoc parameter in http request");
			}
			return updateOntologyMirrorEntry(updateType, baseURI, mirrorFileName, location);

		} else
			return ServletUtilities.getService().createExceptionResponse(request,
					"no handler for such a request!");

	}

	/**
	 * gets the namespace mapping for the loaded ontology
	 * 
	 * <streponse request="getOntologyMirror" type="data">
	 * <Mirror uri="http://xmlns.com/foaf/spec/20070524.rdf" file="foaf.rdf"/>
	 * <Mirror uri="http://sweet.jpl.nasa.gov/ontology/earthrealm.owl" file="earthrealm.owl"/> </Tree>
	 * 
	 */
	public ResponseREPLY getOntologyMirrorTable() {
		Hashtable<Object, Object> mirror = OntologiesMirror.getFullMirror();
		Enumeration<Object> uris = mirror.keys();

		XMLResponseREPLY resp = ServletUtilities.getService().createReplyResponse(getOntologyMirror,
				RepliesStatus.ok);
		Element dataElement = resp.getDataElement();

		while (uris.hasMoreElements()) {
			String uri = (String) uris.nextElement();
			Element mirrorElem = XMLHelp.newElement(dataElement, "Mirror");
			mirrorElem.setAttribute("ns", uri);
			mirrorElem.setAttribute("file", (String) mirror.get(uri));
		}
		return resp;
	}

	/**
	 * deletes an entry (and its associated physical file) from the Ontology Mirror
	 * 
	 * @return
	 */
	public XMLResponse deleteOntologyMirrorEntry(String baseURI, String cacheFileName) {
		OntologiesMirror.removeCachedOntologyEntry(baseURI);
		File cacheFile = new File(Resources.getOntologiesMirrorDir(), cacheFileName);
		cacheFile.delete();

		return ServletUtilities.getService().createReplyResponse(deleteOntMirrorEntry, RepliesStatus.ok,
				"mirror entry removed");
	}

	// TODO transform MirroredOntologyFile into an extended class for java.io.File
	/**
	 * updates an entry (and its associated physical file) from the Ontology Mirror
	 * 
	 * <Tree type="AckMsg"> <Msg content="mirror entry updated"/> </Tree>
	 * 
	 * @return
	 */
	public XMLResponse updateOntologyMirrorEntry(int updateType, String baseURI, String mirrorFileName,
			String location) {

		MirroredOntologyFile mirFile = new MirroredOntologyFile(mirrorFileName);
		try {
			if (updateType == webUpdate) { // use first a temporary file, just in case the download brokes in
				// the middle, then copies the temporary to the destination in the
				// mirror
				File tempFile = Files.createTempFile("updateOntMirrorEntry", "temp").toFile();
				try {
					Utilities.downloadRDF(new URL(location), tempFile.getAbsolutePath());
					Utilities.copy(tempFile.getAbsolutePath(), mirFile.getAbsolutePath());
				} finally {
					tempFile.delete();
				}
			} else if (updateType == localUpdate) {
				Utilities.copy(location, mirFile.getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return ServletUtilities.getService().createExceptionResponse(updateOntMirrorEntry,
					"problems in updating mirrored ontology file:\n" + e.getMessage());
		}

		return ServletUtilities.getService().createReplyResponse(updateOntMirrorEntry, RepliesStatus.ok,
				"mirror entry updated");
	}

}
