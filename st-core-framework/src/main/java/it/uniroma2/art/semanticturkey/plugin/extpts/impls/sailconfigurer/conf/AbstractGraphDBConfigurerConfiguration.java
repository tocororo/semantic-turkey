package it.uniroma2.art.semanticturkey.plugin.extpts.impls.sailconfigurer.conf;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfigurationParameter;

public abstract class AbstractGraphDBConfigurerConfiguration extends AbstractPluginConfiguration
		implements PredefinedSailConfigurerConfiguration {

	@PluginConfigurationParameter(description = "BASE URL (owlim:base-URL)")
	public String baseURL = "";
	
	@PluginConfigurationParameter(description = "Default namespaces for imports(';' delimited) (owlim:defaultNS)")
    public String defaultNS = "";
	
	@PluginConfigurationParameter(description = "Entity index size (owlim:entity-index-size)")
	public int entityIndexSize = 10000000;

	@PluginConfigurationParameter(description = "Entity ID bit-size (owlim:entity-id-size)")
	public int entityIdSize = 32;

	@PluginConfigurationParameter(description = "Imported RDF files(';' delimited) (owlim:imports)")
	public String imports = "";

	@PluginConfigurationParameter(description = "Repository type (owlim:repository-type)")
	public String repositoryType = "file-repository";

	@PluginConfigurationParameter(description = "Rule-set (owlim:rule-set)")
	public String ruleSet = "rdfsplus-optimized";

	@PluginConfigurationParameter(description = "Storage folder (owlim:storage-folder)")
	public String storageFolder = "storage";

	@PluginConfigurationParameter(description = "Use context index (owlim:enable-context-index)")
	public boolean enableContextIndex = false;

	@PluginConfigurationParameter(description = "Use predicate indices (owlim:enablePredicateList)")
	public boolean enablePredicateList = true;

	@PluginConfigurationParameter(description = "Cache literal language tags (owlim:in-memory-literal-properties)")
	public boolean inMemoryLiteralProperties = true;
	
	@PluginConfigurationParameter(description = "Enable literal index (owlim:enable-literal-index)")
	public boolean enableLiteralIndex = true;

	@PluginConfigurationParameter(description = "Check for inconsistencies (owlim:check-for-inconsistencies)")
	public boolean checkForInconsistencies = false;
	
	@PluginConfigurationParameter(description = "Disable OWL sameAs (owlim:disable-sameAs)")
	public boolean disableSameAs = true;

	@PluginConfigurationParameter(description = "Query time-out (seconds) (owlim:query-timeout)")
	public int queryTimeout = 0;

	@PluginConfigurationParameter(description = "Limit query results (seconds) (owlim:query-limit-results)")
	public int queryLimitResults= 0;

	@PluginConfigurationParameter(description = "Throw exception on query time-out (owlim:throw-QueryEvaluationException-on-timeout)")
	public boolean throwQueryEvaluationExceptionOnTimeout = false;

	@PluginConfigurationParameter(description = "Read-only (owlim:read-only)")
	public boolean readOnly = false;
}
