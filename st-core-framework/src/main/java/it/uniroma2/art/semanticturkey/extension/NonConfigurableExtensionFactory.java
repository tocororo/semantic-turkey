package it.uniroma2.art.semanticturkey.extension;

public interface NonConfigurableExtensionFactory<ExtType extends Extension> extends ExtensionFactory<ExtType>  {
	
	/**
	 * Instantiates an extension
	 * 
	 * @param conf
	 * @return
	 */
	ExtType createInstance();
	
	
}
