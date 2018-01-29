package it.uniroma2.art.semanticturkey.extension;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * An ExtensionFactory provides instances of a given {@link Extension}. The metadata provided by the factory
 * actually refers to Extension
 * 
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <EXTTYPE>
 */
public interface ExtensionFactory<EXTTYPE extends Extension> {

	@SuppressWarnings("unchecked")
	default Class<EXTTYPE> getExtensionType() {
		for (Type t : getClass().getGenericInterfaces()) {
			Map<TypeVariable<?>, Type> typeArgs = TypeUtils.getTypeArguments(t, ExtensionFactory.class);
			if (typeArgs != null) {
				for (Entry<TypeVariable<?>, Type> entry : typeArgs.entrySet()) {
					if (entry.getKey().getGenericDeclaration() == ExtensionFactory.class) {
						return (Class<EXTTYPE>) TypeUtils.getRawType(entry.getValue(), null);
					}
				}
			}
		}

		throw new IllegalStateException("Could not determine the extension type");
	}

	/**
	 * returns a short name for the extension
	 * 
	 * @return a short name for the extension
	 */
	String getName();

	/**
	 * returns a description of the extension
	 * 
	 * @return a description of the extension
	 */
	String getDescription();
}
