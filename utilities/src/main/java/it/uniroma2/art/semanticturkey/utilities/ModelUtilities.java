package it.uniroma2.art.semanticturkey.utilities;

public class ModelUtilities {

	
	/**
	 * given namespace <code>namespace</code>, this tries to automatically suggest a prefix for it
	 * 
	 * @param namespace
	 * @return
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

		return tempString.substring(lowerCutIndex + 1);
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
}
