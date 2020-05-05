package it.uniroma2.art.semanticturkey.project;

import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * SHACL-related settings.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class SHACLSettings implements Settings {

	@Override
	public String getShortName() {
		return "SHACL Settings";
	}

	@STProperty(displayName = "Parallel validation", description = "")
	public boolean parallelValidation = ShaclSailConfig.PARALLEL_VALIDATION_DEFAULT;

	@STProperty(displayName = "Undefined target validates all subjects", description = "")
	public boolean undefinedTargetValidatesAllSubjects = ShaclSailConfig.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS_DEFAULT;

	@STProperty(displayName = "Log validation plans", description = "")
	public boolean logValidationPlans = ShaclSailConfig.LOG_VALIDATION_PLANS_DEFAULT;

	@STProperty(displayName = "Log validation violations", description = "")
	public boolean logValidationViolations = ShaclSailConfig.LOG_VALIDATION_VIOLATIONS_DEFAULT;

	@STProperty(displayName = "Cache select nodes", description = "")
	public boolean cacheSelectNodes = ShaclSailConfig.CACHE_SELECT_NODES_DEFAULT;

	@STProperty(displayName = "Global log validation execution", description = "")
	public boolean globalLogValidationExecution = ShaclSailConfig.GLOBAL_LOG_VALIDATION_EXECUTION_DEFAULT;

	@STProperty(displayName = "RDFS subclass reasoning", description = "")
	public boolean rdfsSubclassReasoning = ShaclSailConfig.RDFS_SUB_CLASS_REASONING_DEFAULT;

	@STProperty(displayName = "Performance logging", description = "")
	public boolean performanceLogging = ShaclSailConfig.PERFORMANCE_LOGGING_DEFAULT;

	@STProperty(displayName = "Serializable validation", description = "")
	public boolean serializableValidation = ShaclSailConfig.SERIALIZABLE_VALIDATION_DEFAULT;
}
