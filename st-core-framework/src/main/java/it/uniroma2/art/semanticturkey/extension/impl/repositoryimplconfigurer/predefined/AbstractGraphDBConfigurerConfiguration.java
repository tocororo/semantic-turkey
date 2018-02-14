package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Abstract base class of GraphDB configurations. For information about the configuration properties, see:
 * http://graphdb.ontotext.com/documentation/free/configuring-a-repository.html#configuration-parameters
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class AbstractGraphDBConfigurerConfiguration
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
	public String ruleset = "empty";

	@STProperty(description = "Storage folder (owlim:storage-folder)")
	public String storageFolder = "storage";

	@STProperty(description = "Use context index (owlim:enable-context-index)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean enableContextIndex = false;

	@STProperty(description = "Use predicate indices (owlim:enablePredicateList)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean enablePredicateList = true;

	@STProperty(description = "Cache literal language tags (owlim:in-memory-literal-properties)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean inMemoryLiteralProperties = true;

	@STProperty(description = "Enable literal index (owlim:enable-literal-index)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean enableLiteralIndex = true;

	@STProperty(description = "Check for inconsistencies (owlim:check-for-inconsistencies)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean checkForInconsistencies = false;

	@STProperty(description = "Disable OWL sameAs (owlim:disable-sameAs)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean disableSameAs = true;

	@STProperty(description = "Query time-out (seconds) (owlim:query-timeout)")
	public int queryTimeout = 0;

	@STProperty(description = "Limit query results (seconds) (owlim:query-limit-results)")
	public int queryLimitResults = 0;

	@STProperty(description = "Throw exception on query time-out (owlim:throw-QueryEvaluationException-on-timeout)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean throwQueryEvaluationExceptionOnTimeout = false;

	@STProperty(description = "Read-only (owlim:read-only)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean readOnly = false;
}
