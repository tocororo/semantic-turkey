package it.uniroma2.art.semanticturkey.utilities;

import org.apache.axis.types.NCName;

public class ModelUtilities {

	/**
	 * given namespace <code>namespace</code>, this tries to automatically suggest a prefix for it
	 * 
	 * @param namespace
	 * @return the guessed prefix, or null if the guess was not possible
	 */
	public static String guessPrefix(String namespace) {
		int lowerCutIndex;
		String tempString;

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

		String prefix = tempString.substring(lowerCutIndex + 1);
		return isPrefixSyntValid(prefix) ? prefix : null;
	}

	/**
	 * given the candidate prefix <code>prefix</code>, return true it is syntactically valid,
	 * false otherwise
	 * @param prefix the prefix to be checked if it is syntactically valid
	 * @return true if the prefix is syntactically valid, false otherwise
	 */
	public static boolean isPrefixSyntValid(String prefix){
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
}
