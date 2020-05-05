package it.uniroma2.art.semanticturkey.data.nature;

/**
 * This enumeration defins possible scopes for a triple.
 * <ul>
 * <li><b>local</b>: data in the <em>working graph</em></li>
 * <li><b>staged</b>: data comes from the <em>staging graph</em></li>
 * <li><b>del_staged</b>: data comes from the <em>deletion staging graph</em></li>
 * <li><b>imported</b>: data comes from an imported ontology. Currently, this corresponds to data coming from
 * <em>any graph that is not in the other categories</em></li>
 * <li><b>inferred</b>: data comes was inferred</li>
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public enum TripleScopes {
	local, staged, del_staged, imported, inferred
}
