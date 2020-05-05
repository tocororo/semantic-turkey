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
 * (ai-nlp.info.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about SemanticTurkey can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class realizes a File-persisted manager for namespace-prefix mappings, adopted inside Semantic Turkey
 * projects<br/>
 * This system has an in-memory representation of the map, which is regularly persisted to file; its sync is
 * managed internally. On the contrary, this manager does not syncs automatically with the prefix-namespace
 * mappings held inside an RDF4J repository. This latter sync is managed by the
 * {@link {@link STOntologyManager}
 * 
 * @author Armando Stellato
 * @author Manuel Fiorelli
 */
public class NSPrefixMappings {
	public static final String prefixMappingFileName = "PrefixMappings.xml";

	private final Properties namespacePrefixMap;
	private final Map<String, String> prefixNamespaceMap;
	private final File nsPrefixMappingFile;
	private final boolean persistMode;
	private final ReentrantReadWriteLock rwLock;

	// protected static Logger logger = LoggerFactory.getLogger(NSPrefixMappings.class);

	public NSPrefixMappings(File persistenceDirectory, boolean persistMode) throws IOException {
		this.persistMode = persistMode;
		namespacePrefixMap = new Properties();
		prefixNamespaceMap = new HashMap<>();
		nsPrefixMappingFile = new File(persistenceDirectory, "/" + prefixMappingFileName);
		try {
			FileInputStream input = new FileInputStream(nsPrefixMappingFile);
			namespacePrefixMap.load(input);
			Set<Object> namespaces = namespacePrefixMap.keySet();
			for (Object ns : namespaces)
				prefixNamespaceMap.put(namespacePrefixMap.getProperty((String) ns), (String) ns);
			input.close();
		} catch (FileNotFoundException e1) {
			nsPrefixMappingFile.createNewFile();
		}
		rwLock = new ReentrantReadWriteLock(true);
	}

	public void updatePrefixMappingRegistry() throws NSPrefixMappingUpdateException {
		rwLock.writeLock().lock();
		try {
			FileOutputStream os;
			try {
				os = new FileOutputStream(nsPrefixMappingFile);
				// properties.storeToXML(os, "local cache references for mirroring remote ontologies");
				namespacePrefixMap.store(os,
						"local cache references mappings between prefixes and namespace");
				os.close();
			} catch (FileNotFoundException e) {
				throw new NSPrefixMappingUpdateException(
						"synchronization with persistent namespace-prefix mapping repository failed; mappings may result different upon reloading the ontology");
			} catch (IOException e) {
				throw new NSPrefixMappingUpdateException(
						"synchronization with persistent namespace-prefix mapping repository failed; mappings may result different upon reloading the ontology");
			}
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	public void setNSPrefixMapping(String namespace, String newPrefix) throws NSPrefixMappingUpdateException {
		rwLock.writeLock().lock();
		try {
			String oldPrefix = namespacePrefixMap.getProperty(namespace);
			if (oldPrefix != null)
				prefixNamespaceMap.remove(oldPrefix);
			namespacePrefixMap.setProperty(namespace, newPrefix);
			prefixNamespaceMap.put(newPrefix, namespace);
			if (persistMode)
				updatePrefixMappingRegistry();
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	public void removeNSPrefixMapping(String namespace) throws NSPrefixMappingUpdateException {
		rwLock.writeLock().lock();
		try {
			String prefix = namespacePrefixMap.getProperty(namespace);
			if (prefix == null)
				throw new NSPrefixMappingUpdateException(
						"inconsistency error: prefix-mapping table does not contain this namespace");
			namespacePrefixMap.remove(namespace);
			prefixNamespaceMap.remove(prefix);
			if (persistMode)
				updatePrefixMappingRegistry();
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	public String getNamespaceFromPrefix(String prefix) {
		rwLock.readLock().lock();
		try {
			return (String) prefixNamespaceMap.get(prefix);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	public String getPrefixFromNamespace(String namespace) {
		rwLock.readLock().lock();
		try {
			return (String) namespacePrefixMap.get(namespace);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	/**
	 * @return a freshly created <code>map</code> with prefixes as keys and namespaces as values
	 */
	public Map<String, String> getNSPrefixMappingTable() {
		rwLock.readLock().lock();
		try {
			return new HashMap<>(prefixNamespaceMap);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	public void clearNSPrefixMappings() throws NSPrefixMappingUpdateException {
		rwLock.writeLock().lock();
		try {
			prefixNamespaceMap.clear();
			if (persistMode)
				updatePrefixMappingRegistry();
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	/**
	 * @return the file where prefix mappings customized by the user are being persisted
	 */
	public File getFile() {
		return nsPrefixMappingFile;
	}

}
