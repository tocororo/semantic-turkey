package it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Abstract base class of GraphDB configurations. For information about the configuration properties, see:
 * http://graphdb.ontotext.com/documentation/free/configuring-a-repository.html#configuration-parameters
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class AbstractGraphDBConfigurerConfiguration extends AbstractPluginConfiguration
		implements PredefinedRepositoryImplConfigurerConfiguration {

	@STProperty(description = "BASE URL (owlim:base-URL)")
	public String baseURL = "";

	@STProperty(description = "Default namespaces for imports(';' delimited) (owlim:defaultNS)")
	public String defaultNS = "";

	@STProperty(description = "Entity index size (owlim:entity-index-size)")
	public int entityIndexSize = 10000000;

	@STProperty(description = "Entity ID bit-size (owlim:entity-id-size)")
	@Enumeration({ "32", "40" })
	public int entityIdSize = 32;

	@STProperty(description = "Imported RDF files(';' delimited) (owlim:imports)")
	public String imports = "";

	@STProperty(description = "Repository type (owlim:repository-type)")
	@Enumeration({ "file-repository", "weighted-file-repository" })
	public String repositoryType = "file-repository";

	@STProperty(description = "Rule-set (owlim:ruleset)")
	@Enumeration({ "empty", "rdfs", "rdfsplus", "owl-horst", "owl-max", "owl2-rl", "rdfs-optimized",
			"rdfsplus-optimized", "owl-horst-optimized", "owl-max-optimized", "owl2-rl-optimized" })
	public String ruleset = "rdfsplus-optimized";

	@STProperty(description = "Storage folder (owlim:storage-folder)")
	public String storageFolder = "storage";

	@STProperty(description = "Use context index (owlim:enable-context-index)")
	public boolean enableContextIndex = false;

	@STProperty(description = "Use predicate indices (owlim:enablePredicateList)")
	public boolean enablePredicateList = true;

	@STProperty(description = "Cache literal language tags (owlim:in-memory-literal-properties)")
	public boolean inMemoryLiteralProperties = true;

	@STProperty(description = "Enable literal index (owlim:enable-literal-index)")
	public boolean enableLiteralIndex = true;

	@STProperty(description = "Check for inconsistencies (owlim:check-for-inconsistencies)")
	public boolean checkForInconsistencies = false;

	@STProperty(description = "Disable OWL sameAs (owlim:disable-sameAs)")
	public boolean disableSameAs = true;

	@STProperty(description = "Query time-out (seconds) (owlim:query-timeout)")
	public int queryTimeout = 0;

	@STProperty(description = "Limit query results (seconds) (owlim:query-limit-results)")
	public int queryLimitResults = 0;

	@STProperty(description = "Throw exception on query time-out (owlim:throw-QueryEvaluationException-on-timeout)")
	public boolean throwQueryEvaluationExceptionOnTimeout = false;

	@STProperty(description = "Read-only (owlim:read-only)")
	public boolean readOnly = false;
}
