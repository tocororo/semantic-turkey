package it.uniroma2.art.semanticturkey.extension;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <EXTTYPE>
 */
public interface NonConfigurableExtensionFactory<EXTTYPE extends Extension> extends ExtensionFactory<EXTTYPE>  {
	
	/**
	 * Instantiates an extension
	 * 
	 * @param conf
	 * @return
	 */
	EXTTYPE createInstance();
	
	
}
