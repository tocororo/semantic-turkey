package it.uniroma2.art.semanticturkey.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.axis.types.NCName;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelUtilities {

	protected static Logger logger = LoggerFactory.getLogger(ModelUtilities.class);

	private static String fileResourcesLocation = "it/uniroma2/art/semanticturkey/prefixes/";
	private static String prefiFileLocation = fileResourcesLocation + "prefixCC.json";

	/**
	 * given namespace <code>namespace</code>, this tries to automatically suggest a prefix for it
	 * 
	 * @param namespace
	 * @param namespaces
	 * @return the guessed prefix, or null if the guess was not possible
	 */
	public static String guessPrefix(String namespace, RepositoryResult<Namespace> namespaces) {
		//int lowerCutIndex;
		//String tempString;
		String prefix=null;


		//prepare a map for the already existing prefix-namespace (and during the construction of this map, check if
		// the input namespace does not already have a prefix, in that case return null, since there is no need to
		// generate a new prefix for this namespace)
		Map<String, String> prefixToNamespaceMap = new HashMap<>();
		Iterator<Namespace> iter = namespaces.stream().iterator();
		while(iter.hasNext()){
			Namespace namespaceStruct = iter.next();
			if(namespaceStruct.getName().equals(namespace)){
				//the namespace is already present among the namespace having a prefix, so return null
				return null;
			}
			prefixToNamespaceMap.put(namespaceStruct.getPrefix(), namespaceStruct.getName());
		}

		//first try to get the prefix from the file taken by prefix.cc via http://prefix.cc/popular/all.file.json
		try {
			prefix = getPrefixFromNamespaceFile(namespace);
		} catch (IOException e) {
			//there was a problem in reading the file, so set the prefix to null
			prefix = null;
			logger.error("Problem with the prefixCC file: "+e.getMessage());
		}
		if(prefix!=null && !prefix.isEmpty() && isPrefixSyntValid(prefix)) {
			return prefix;
		}

		// the prefix was not found in the prefix-namespace file, so try to automatically generate one
		String[] nsArray = namespace.split("/");
		for(int i=nsArray.length-1; i>0; --i){
			String nsPart = nsArray[i];
			nsPart = nsPart.replace("#", "");
			if(nsPart.isEmpty()){
				// an empty string, so skip it
				continue;
			}
			// check if the prefix is valid
			if(isPrefixSyntValid(nsPart)){
				// check if the prefix is not already present
				if(prefixToNamespaceMap.get(nsPart)==null) {
					//the proposed prefix is not already presnt in the map, so it can be used
					return nsPart;
				}
			}
		}
		return null;

		/*
		// OLD guess algorithm
		if (namespace.endsWith("/") || namespace.endsWith("#"))
			tempString = namespace.substring(0, namespace.length() - 1);
		else
			tempString = namespace;

		if (tempString.matches(".*\\.(\\w{2}|\\w{3})"))
			tempString = tempString.substring(0, tempString.lastIndexOf("."));

		int pointLowerCutIndex = tempString.lastIndexOf(".");
		int slashLowerCutIndex = tempString.lastIndexOf("/");
		if (pointLowerCutIndex > slashLowerCutIndex)
			lowerCutIndex = pointLowerCutIndex;
		else
			lowerCutIndex = slashLowerCutIndex;

		prefix = tempString.substring(lowerCutIndex + 1);
		return isPrefixSyntValid(prefix) ? prefix : null;
		*/
	}

	public static String getNamespaceFormPrefixFromFile(String prefix) throws  IOException {
		return getPrefixOrNamespaceFromFile(prefix, null);
	}

	public static String getPrefixFromNamespaceFile(String namespace) throws IOException {
		return getPrefixOrNamespaceFromFile(null, namespace);
	}

	private static String getPrefixOrNamespaceFromFile(String prefix, String namespace) {
		if(prefix == null && namespace == null){
			return null;
		} else if(prefix != null && namespace !=null){
			return null;
		}

		StringBuffer sb = new StringBuffer();
		//try (BufferedReader reader = new BufferedReader(new FileReader(prefiFileLocation))) {

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(
						ModelUtilities.class.getClassLoader().getResourceAsStream(prefiFileLocation))))) {
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				sb.append("\n").append(currentLine);
			}
		} catch (IOException e) {
			logger.error("Problem with reading the prefixCC file: "+e.getMessage());
			return null;
		}

		ObjectNode objectNode = null;
		try {
			objectNode = (ObjectNode) new ObjectMapper().readTree(sb.toString());
		} catch (IOException e) {
			logger.error("Problem with parsing the prefixCC JSON file: "+e.getMessage());
			return null;
		}
		//now iterate over the elements (prefix/namespace) read from the file
		String foundText = null;
		for (Iterator<String> it = objectNode.fieldNames(); it.hasNext(); ) {
			String prefixFile = it.next();
			String namespaceFile = objectNode.get(prefixFile).asText();
			if(prefix != null) {
				if(prefix.equals(prefixFile)){
					foundText = namespaceFile;
				}
			} else {
				if(namespace.equals(namespaceFile)) {
					foundText = prefixFile;
				}
			}
			if(foundText!=null){
				//found what was seraching for, so stop reading the data
				return foundText;
			}
		}
		return  null;
	}

	/**
	 * given the candidate prefix <code>prefix</code>, return true it is syntactically valid, false otherwise
	 * 
	 * @param prefix
	 *            the prefix to be checked if it is syntactically valid
	 * @return true if the prefix is syntactically valid, false otherwise
	 */
	public static boolean isPrefixSyntValid(String prefix) {
		return NCName.isValid(prefix);
	}

	/**
	 * given the baseuri of an ontology, returns the baseuri
	 * 
	 * @param baseuri
	 * @return
	 */
	public static String createDefaultNamespaceFromBaseURI(String baseuri) {
		if (baseuri.endsWith("/") || baseuri.endsWith("#"))
			return baseuri;
		else
			return baseuri + "#";
	}

	/**
	 * Extends the given namespace {@code ns} with the supplied string {@code ext}.
	 * 
	 * @param ns
	 * @param ext
	 * @return
	 */
	public static String extendNamespace(String ns, String ext) {
		if (ns.endsWith("#")) {
			return ns.substring(0, ns.length() - 1) + "/" + ext;
		} else if (ns.endsWith("/")) {
			return ns + ext;
		} else {
			return ns + "/" + ext;
		}
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}. This is a convenience overload of
	 * {@link #toLiteral(Value, Map, ValueFactory)} that uses {@link SimpleValueFactory}.
	 * 
	 * @param value
	 * @param prefixDeclarations
	 * @return
	 */
	public static Literal toLiteral(Value value, Map<String, String> prefixDeclarations) {
		return toLiteral(value, prefixDeclarations, SimpleValueFactory.getInstance());
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}.This is a convenience overload of
	 * {@link #toLiteral(Value, Map, ValueFactory)} that uses no prefix declaration and a
	 * {@link SimpleValueFactory}.
	 * 
	 * @param value
	 * @return
	 */
	public static Literal toLiteral(Value value) {
		return toLiteral(value, Collections.emptyMap(), SimpleValueFactory.getInstance());
	}

	/**
	 * Converts an RDF4J {@link Value} to a {@link Literal}. If the argument is a kind of {@link Resource},
	 * the {@link Resource#toString()} is used as label of the produced literal, possibly shortening URIs with
	 * the supplied prefix declarations.
	 * 
	 * @param value
	 * @param prefixDeclarations
	 * @param vf
	 * @return
	 */
	public static Literal toLiteral(Value value, Map<String, String> prefixDeclarations, ValueFactory vf) {
		if (value instanceof Literal) {
			return (Literal) value;
		} else {
			String label;
			if (value instanceof IRI) {
				label = getQName((IRI) value, prefixDeclarations);
			} else {
				label = value.stringValue();
			}
			return vf.createLiteral(label);
		}
	}

	/**
	 * Returns a qname representing the supplied IRI. If no prefix declaration applies, then the original IRI
	 * is returned.
	 * 
	 * @param input
	 * @param prefixDeclarations
	 * @return
	 */
	public static String getQName(IRI input, Map<String, String> prefixDeclarations) {
		return prefixDeclarations.entrySet().stream()
				.filter(e -> Objects.equals(input.getNamespace(), e.getValue())).map(Map.Entry::getKey)
				.map(prefix -> prefix + ":" + input.getLocalName()).findAny().orElse(input.stringValue());
	}
}
