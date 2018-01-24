package it.uniroma2.art.semanticturkey.extension;

/**
 * An ExtensionFactory provides instances of a given {@link Extension}.
 * The metadata provided by the factory actually refers to Extension
 * 
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <EXTTYPE>
 */
public interface ExtensionFactory<EXTTYPE extends Extension> {

	// TODO implement with reflection as a default method in the interface
	Class<EXTTYPE> getExtensionType();	
	
	/**
	 * returns a short name for the extension  
	 * 
	 * @return a short name for the extension
	 */
	String getName();
	
	/**
	 * returns a description of the extension
	 * 
	 * 	@return a description of the extension  
	 */
	String getDescription();
	
	
}
