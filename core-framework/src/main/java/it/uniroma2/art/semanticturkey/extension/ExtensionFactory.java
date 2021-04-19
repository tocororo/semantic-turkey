package it.uniroma2.art.semanticturkey.extension;

import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;
import org.apache.commons.lang3.ClassUtils;

import java.util.List;

import static java.util.stream.Collectors.toList;

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
public interface ExtensionFactory<EXTTYPE extends Extension> extends IdentifiableComponent {

	@Override
	default String getId() {
		return getExtensionType().getName();
	}

	default Class<EXTTYPE> getExtensionType() {
		return ReflectionUtilities.getInterfaceArgumentTypeAsClass(this.getClass(), ExtensionFactory.class,
				0);
	}

	default List<Class<? extends Extension>> getInterfaces() {
		return ClassUtils.getAllInterfaces(getExtensionType()).stream()
				.filter(Extension.class::isAssignableFrom)
				.filter(c -> !c.equals(Extension.class))
				.map(c -> (Class<? extends Extension>)c).collect(toList());
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
