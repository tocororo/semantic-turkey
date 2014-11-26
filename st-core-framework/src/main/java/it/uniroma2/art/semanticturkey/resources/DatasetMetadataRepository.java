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

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.owlart.vocabulary.SKOSXL;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.rendering.RDFSRenderingEngine;
import it.uniroma2.art.semanticturkey.rendering.SKOSXLRenderingEngine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A repository of metadata about known datasets.
 * 
 */
public class DatasetMetadataRepository {
	private static DatasetMetadataRepository defaultInstance = new DatasetMetadataRepository();

	/**
	 * Returns the default metadata repository instance.
	 * 
	 * @return
	 */
	public static DatasetMetadataRepository getInstance() {
		return defaultInstance;
	}

	private Map<String, DatasetMetadata> datasetMetadataRepository;

	protected DatasetMetadataRepository() {
		this.datasetMetadataRepository = new HashMap<String, DatasetMetadata>();

		// TODO: remove this initialization block defining some known datasets
		datasetMetadataRepository.put("http://dbpedia.org/resource/", new DatasetMetadata(
				"http://dbpedia.org/resource/", null, "http://dbpedia.org/sparql", true,
				new RDFSRenderingEngine(), RDFS.Res.URI));
		datasetMetadataRepository.put("http://aims.fao.org/aos/agrovoc/", new DatasetMetadata(
				"http://aims.fao.org/aos/agrovoc/", null, "http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc#", true, new SKOSXLRenderingEngine(), SKOSXL.Res.URI));
		datasetMetadataRepository.put("http://lod.nal.usda.gov/nalt/", new DatasetMetadata(
				"http://lod.nal.usda.gov/nalt/", null, null, true, new RDFSRenderingEngine(), SKOSXL.Res.URI));

	}

	public DatasetMetadata findDatasetForResource(ARTURIResource uriResource) {
		//-------------------------------------------------------------------------------------------
		// The following resolution strategy might be subject to ambiguity in some rare circumstances
		
		DatasetMetadata datasetMetadata;
		
		//-----------------------------------------
		// Case 1: The provided URI is the base URI
		
		datasetMetadata = datasetMetadataRepository.get(uriResource.getURI());

		if (datasetMetadata != null) {
			return datasetMetadata;
		}

		//------------------------------------------
		// Case 2: The namespace is the base URI
		//         e.g., [http://example.org/]Person

		String namespace = uriResource.getNamespace();

		datasetMetadata = datasetMetadataRepository.get(namespace);

		if (datasetMetadata != null) {
			return datasetMetadata;
		}
		
		//--------------------------------------------
		// Case 2: The namespace is the base URI + "#"
		//         e.g., [http://example.org]#Person


		if (namespace.endsWith("#")) {
			datasetMetadata = datasetMetadataRepository.get(namespace.substring(0, namespace.length() - 1));
			return datasetMetadata;
		} else {
			return null;
		}

	}
}
