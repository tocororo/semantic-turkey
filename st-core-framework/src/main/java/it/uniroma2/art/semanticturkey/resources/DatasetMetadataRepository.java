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
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.rdf4j.model.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;

/**
 * A repository of metadata about known datasets.
 * 
 */
public class DatasetMetadataRepository {

	private static final String DATASET_METADATA_REPOSITORY_DIRECTORY = "datasetMetadataRepository";
	private static final String DATASET_METADATA_REPOSITORY_FILE = "repository.xml";

	private File repoDirectory;
	private File repoFile;

	private Map<String, DatasetMetadata> base2meta;

	/**
	 * Constructs a {@link DatasetMetadataRepository} whose base is {@link Config#getDataDir()}.
	 * 
	 * @throws DatasetMetadataRepositoryCreationException
	 */
	public DatasetMetadataRepository() throws DatasetMetadataRepositoryCreationException {
		this(Config.getDataDir());
	}

	/**
	 * Constructs a {@link DatasetMetadataRepository} based on the file (if any)
	 * <code>$baseDir/datasetMetadataRepository/repository.ttl</code>.
	 * 
	 * @param baseDir
	 * @throws DatasetMetadataRepositoryCreationException
	 */
	public DatasetMetadataRepository(File baseDir) throws DatasetMetadataRepositoryCreationException {
		try {
			this.repoDirectory = new File(baseDir, DATASET_METADATA_REPOSITORY_DIRECTORY);
			this.repoFile = new File(repoDirectory, DATASET_METADATA_REPOSITORY_FILE);
			this.repoDirectory.mkdirs();

			if (!this.repoDirectory.exists()) {
				throw new DatasetMetadataRepositoryCreationException(
						"Cannot create the folder hierarchy associated with the dataset metadata repository");
			}

			base2meta = parseRepositoryFile(this.repoFile);
		} catch (FactoryConfigurationError | ParserConfigurationException | SAXException | IOException
				| XPathExpressionException e) {
			throw new DatasetMetadataRepositoryCreationException(e);
		}
	}

	private static Map<String, DatasetMetadata> parseRepositoryFile(File repoFile)
			throws ParserConfigurationException, SAXException, IOException,
			DatasetMetadataRepositoryCreationException, XPathExpressionException {
		Map<String, DatasetMetadata> result = new HashMap<String, DatasetMetadata>();

		if (!repoFile.exists()) {
			return result;
		}

		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(repoFile);

		Element docElement = doc.getDocumentElement();

		if (docElement == null || !docElement.getTagName().equals("repository")) {
			throw new DatasetMetadataRepositoryCreationException("Missing document element \"repository\"");
		}

		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList datasetNodes = (NodeList) xPath.evaluate("/repository/dataset", docElement,
				XPathConstants.NODESET);
		for (int i = 0; i < datasetNodes.getLength(); ++i) {
			Element datasetElement = (Element) datasetNodes.item(i);

			String datasetBaseURI = datasetElement.getAttribute("baseURI").trim();

			String datasetTitle = (String) xPath.evaluate("title[1]/text()", datasetElement,
					XPathConstants.STRING);
			if (datasetTitle != null) {
				datasetTitle = datasetTitle.trim();
			}

			String sparqlEndpoint = (String) xPath.evaluate("sparqlEndpoint[1]/text()", datasetElement,
					XPathConstants.STRING);
			if (sparqlEndpoint != null) {
				sparqlEndpoint = sparqlEndpoint.trim();
			}

			boolean dereferenceable = Objects.equal("true",
					xPath.evaluate("dereferenceable[1]/text()", datasetElement, XPathConstants.STRING));

			result.put(datasetBaseURI, new DatasetMetadata(datasetBaseURI, datasetTitle, null,
					sparqlEndpoint, dereferenceable, null, RDFS.Res.URI));
		}

		return result;
	}

	public synchronized void writeBackToFile() throws DatasetMetadataRepositoryWritingException {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			Element repoElement = doc.createElement("repository");
			doc.appendChild(repoElement);

			for (DatasetMetadata meta : base2meta.values()) {
				Element metaElement = XMLHelp.newElement(repoElement, "dataset");
				metaElement.setAttribute("baseURI", meta.getBaseURI());

				String title = meta.getTitle();
				if (title != null) {
					XMLHelp.newElement(metaElement, "title", title);
				}

				String sparqlEndpoint = meta.getSparqlEndpoint();
				if (sparqlEndpoint != null) {
					XMLHelp.newElement(metaElement, "sparqlEndpoint", sparqlEndpoint);
				}

				XMLHelp.newElement(metaElement, "dereferenceable", Boolean.toString(meta.isDereferenceable()));
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(repoFile);
			transformer.transform(source, result);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new DatasetMetadataRepositoryWritingException(e);
		}
	}

	/**
	 * Adds metadata about a dataset
	 * 
	 * @param meta
	 * @throws DuplicateDatasetMetadataException
	 */
	public synchronized void addDatasetMetadata(DatasetMetadata meta)
			throws DuplicateDatasetMetadataException {
		if (base2meta.containsKey(meta.getBaseURI())) {
			throw new DuplicateDatasetMetadataException(meta.getBaseURI());
		}
		base2meta.put(meta.getBaseURI(), meta);
	}

	/**
	 * Deletes the metadata about the dataset identified by the given base URI
	 * 
	 * @param baseURI
	 * @throws NoSuchDatasetMetadataException
	 */
	public synchronized void deleteDatasetMetadata(String baseURI) throws NoSuchDatasetMetadataException {
		DatasetMetadata rv = base2meta.remove(baseURI);

		if (rv == null) {
			throw new NoSuchDatasetMetadataException(baseURI);
		}
	}

	/**
	 * Replaces the metadata about a dataset. The parameter <code>baseURI</code> holds the identifier of the
	 * dataset being edited. If that parameter does not coincide with <code>meta.getBaseURI()</code>, then the
	 * dataset is renamed.
	 * 
	 * @param baseURI
	 * @param meta
	 * @throws NoSuchDatasetMetadataException
	 * @throws DuplicateDatasetMetadataException
	 */
	public synchronized void replaceDatasetMetadata(String baseURI, DatasetMetadata meta)
			throws NoSuchDatasetMetadataException, DuplicateDatasetMetadataException {
		if (!base2meta.containsKey(baseURI)) {
			throw new NoSuchDatasetMetadataException(baseURI);
		}

		if (!Objects.equal(baseURI, meta.getBaseURI())) {
			if (base2meta.containsKey(meta.getBaseURI())) {
				throw new DuplicateDatasetMetadataException(meta.getBaseURI());
			}
			base2meta.remove(baseURI);
		}
		base2meta.put(meta.getBaseURI(), meta);
	}

	/**
	 * Returns a collection comprising all available dataset metadata
	 * 
	 * @return
	 */
	public synchronized Collection<DatasetMetadata> getAllDatasetMetadata() {
		return base2meta.values();
	}

	/**
	 * Returns the metadata about the dataset identified by the given base URI, or <code>null</code> if that
	 * dataset is unknown.
	 * 
	 * @param baseURI
	 * @return
	 */
	public synchronized DatasetMetadata getDatasetMetadata(String baseURI) {
		return base2meta.get(baseURI);
	}

	/**
	 * Returns metadata about the dataset identified by the given URI. If no dataset is found, then the method
	 * returns <code>null</code>.
	 * 
	 * @param uriResource
	 * @return
	 */
	public synchronized DatasetMetadata findDatasetForResource(IRI iriResource) {
		// -------------------------------------------------------------------------------------------
		// The following resolution strategy might be subject to ambiguity in some rare circumstances

		DatasetMetadata datasetMetadata;

		// -----------------------------------------
		// Case 1: The provided URI is the base URI

		datasetMetadata = base2meta.get(iriResource.stringValue());

		if (datasetMetadata != null) {
			return datasetMetadata;
		}

		// ------------------------------------------
		// Case 2: The namespace is the base URI
		// e.g., [http://example.org/]Person

		String namespace = iriResource.getNamespace();

		datasetMetadata = base2meta.get(namespace);

		if (datasetMetadata != null) {
			return datasetMetadata;
		}

		// --------------------------------------------
		// Case 2: The namespace is the base URI + "#"
		// e.g., [http://example.org]#Person

		if (namespace.endsWith("#")) {
			datasetMetadata = base2meta.get(namespace.substring(0, namespace.length() - 1));
			return datasetMetadata;
		} else {
			return null;
		}

	}
	
	/**
	 * Returns metadata about the dataset identified by the given URI. If no dataset is found, then the method
	 * returns <code>null</code>.
	 * 
	 * @param uriResource
	 * @return
	 */
	@Deprecated
	public synchronized DatasetMetadata findDatasetForResource(ARTURIResource uriResource) {
		// -------------------------------------------------------------------------------------------
		// The following resolution strategy might be subject to ambiguity in some rare circumstances

		DatasetMetadata datasetMetadata;

		// -----------------------------------------
		// Case 1: The provided URI is the base URI

		datasetMetadata = base2meta.get(uriResource.getURI());

		if (datasetMetadata != null) {
			return datasetMetadata;
		}

		// ------------------------------------------
		// Case 2: The namespace is the base URI
		// e.g., [http://example.org/]Person

		String namespace = uriResource.getNamespace();

		datasetMetadata = base2meta.get(namespace);

		if (datasetMetadata != null) {
			return datasetMetadata;
		}

		// --------------------------------------------
		// Case 2: The namespace is the base URI + "#"
		// e.g., [http://example.org]#Person

		if (namespace.endsWith("#")) {
			datasetMetadata = base2meta.get(namespace.substring(0, namespace.length() - 1));
			return datasetMetadata;
		} else {
			return null;
		}

	}

}
