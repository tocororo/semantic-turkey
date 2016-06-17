package it.uniroma2.art.semanticturkey.services;

/**
 * Different species of service.
 * <ul>
 * 	<li><em>old-style</em>: <code>Cls</code></li>
 *  <li><em>new-style</em>: <code>ResourceView</code></li>
 *  <li><em>newer new-style</em>: under development</li>
 * </ul>
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public enum ServiceSpecies {
	OldStyle, NewStyle, NewerNewStyle ;
	
	public static ServiceSpecies speciesOf(Object bean) {
		if (bean instanceof NewStyleService) {
			return ServiceSpecies.NewStyle;
		} else if (bean instanceof NewerNewStyleService) {
			return ServiceSpecies.NewerNewStyle;
		}
		
		throw new IllegalArgumentException("Could not determine service nature of provided bean: " + bean);
	}
};
