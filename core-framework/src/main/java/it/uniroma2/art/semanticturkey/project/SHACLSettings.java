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

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.project.SHACLSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String parallelValidation$description = keyBase
				+ ".parallelValidation.description";
		public static final String parallelValidation$displayName = keyBase
				+ ".parallelValidation.displayName";
		public static final String undefinedTargetValidatesAllSubjects$description = keyBase
				+ ".undefinedTargetValidatesAllSubjects.description";
		public static final String undefinedTargetValidatesAllSubjects$displayName = keyBase
				+ ".undefinedTargetValidatesAllSubjects.displayName";
		public static final String logValidationPlans$description = keyBase
				+ ".logValidationPlans.description";
		public static final String logValidationPlans$displayName = keyBase
				+ ".logValidationPlans.displayName";
		public static final String logValidationViolations$description = keyBase
				+ ".logValidationViolations.description";
		public static final String logValidationViolations$displayName = keyBase
				+ ".logValidationViolations.displayName";
		public static final String ignoreNoShapesLoadedException$description = keyBase
				+ ".ignoreNoShapesLoadedException.description";
		public static final String ignoreNoShapesLoadedException$displayName = keyBase
				+ ".ignoreNoShapesLoadedException.displayName";
		public static final String cacheSelectNodes$description = keyBase + ".cacheSelectNodes.description";
		public static final String cacheSelectNodes$displayName = keyBase + ".cacheSelectNodes.displayName";
		public static final String validationEnabled$displayName = keyBase + ".validationEnabled.displayName";
		public static final String validationEnabled$description = keyBase + ".validationEnabled.description";
		public static final String globalLogValidationExecution$description = keyBase
				+ ".globalLogValidationExecution.description";
		public static final String globalLogValidationExecution$displayName = keyBase
				+ ".globalLogValidationExecution.displayName";
		public static final String rdfsSubclassReasoning$description = keyBase
				+ ".rdfsSubclassReasoning.description";
		public static final String rdfsSubclassReasoning$displayName = keyBase
				+ ".rdfsSubclassReasoning.displayName";
		public static final String performanceLogging$description = keyBase
				+ ".performanceLogging.description";
		public static final String performanceLogging$displayName = keyBase
				+ ".performanceLogging.displayName";
		public static final String serializableValidation$description = keyBase
				+ ".serializableValidation.description";
		public static final String serializableValidation$displayName = keyBase
				+ ".serializableValidation.displayName";
		public static final String eclipseRdf4jShaclExtensions$description = keyBase
				+ ".eclipseRdf4jShaclExtensions.description";
		public static final String eclipseRdf4jShaclExtensions$displayName = keyBase
				+ ".eclipseRdf4jShaclExtensions.displayName";
		public static final String dashDataShapes$description = keyBase
				+ ".dashDataShapes.description";
		public static final String dashDataShapes$displayName = keyBase
				+ ".dashDataShapes.displayName";
		public static final String validationResultsLimitTotal$description = keyBase
				+ ".validationResultsLimitTotal.description";
		public static final String validationResultsLimitTotal$displayName = keyBase
				+ ".validationResultsLimitTotal.displayName";
		public static final String validationResultsLimitPerConstraint$description = keyBase
				+ ".validationResultsLimitPerConstraint.description";
		public static final String validationResultsLimitPerConstraint$displayName = keyBase
				+ ".validationResultsLimitPerConstraint.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(displayName = "{" + MessageKeys.parallelValidation$displayName + "}", description = "{"
			+ MessageKeys.parallelValidation$description + "}")
	public boolean parallelValidation = ShaclSailConfig.PARALLEL_VALIDATION_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.undefinedTargetValidatesAllSubjects$displayName
			+ "}", description = "{" + MessageKeys.undefinedTargetValidatesAllSubjects$description + "}")
	public boolean undefinedTargetValidatesAllSubjects = ShaclSailConfig.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.logValidationPlans$displayName + "}", description = "{"
			+ MessageKeys.logValidationPlans$description + "}")
	public boolean logValidationPlans = ShaclSailConfig.LOG_VALIDATION_PLANS_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.logValidationViolations$displayName + "}", description = "{"
			+ MessageKeys.logValidationViolations$description + "}")
	public boolean logValidationViolations = ShaclSailConfig.LOG_VALIDATION_VIOLATIONS_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.ignoreNoShapesLoadedException$displayName + "}", description = "{"
			+ MessageKeys.ignoreNoShapesLoadedException$description + "}")
	public boolean ignoreNoShapesLoadedException = ShaclSailConfig.IGNORE_NO_SHAPES_LOADED_EXCEPTION_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.validationEnabled$displayName + "}", description = "{"
			+ MessageKeys.validationEnabled$description + "}")
	public boolean validationEnabled = ShaclSailConfig.VALIDATION_ENABLED_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.cacheSelectNodes$displayName + "}", description = "{"
			+ MessageKeys.cacheSelectNodes$description + "}")
	public boolean cacheSelectNodes = ShaclSailConfig.CACHE_SELECT_NODES_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.globalLogValidationExecution$displayName
			+ "}", description = "{" + MessageKeys.globalLogValidationExecution$description + "}")
	public boolean globalLogValidationExecution = ShaclSailConfig.GLOBAL_LOG_VALIDATION_EXECUTION_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.rdfsSubclassReasoning$displayName + "}", description = "{"
			+ MessageKeys.rdfsSubclassReasoning$description + "}")
	public boolean rdfsSubclassReasoning = ShaclSailConfig.RDFS_SUB_CLASS_REASONING_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.performanceLogging$displayName + "}", description = "{"
			+ MessageKeys.performanceLogging$description + "}")
	public boolean performanceLogging = ShaclSailConfig.PERFORMANCE_LOGGING_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.serializableValidation$displayName + "}", description = "{"
			+ MessageKeys.serializableValidation$description + "}")
	public boolean serializableValidation = ShaclSailConfig.SERIALIZABLE_VALIDATION_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.eclipseRdf4jShaclExtensions$displayName + "}", description = "{"
			+ MessageKeys.eclipseRdf4jShaclExtensions$description + "}")
	public boolean eclipseRdf4jShaclExtensions = ShaclSailConfig.ECLIPSE_RDF4J_SHACL_EXTENSIONS_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.dashDataShapes$displayName + "}", description = "{"
			+ MessageKeys.dashDataShapes$description + "}")
	public boolean dashDataShapes = ShaclSailConfig.DASH_DATA_SHAPES_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.validationResultsLimitTotal$displayName + "}", description = "{"
			+ MessageKeys.validationResultsLimitTotal$description + "}")
	public long validationResultsLimitTotal = ShaclSailConfig.VALIDATION_RESULTS_LIMIT_TOTAL_DEFAULT;

	@STProperty(displayName = "{" + MessageKeys.validationResultsLimitPerConstraint$displayName + "}", description = "{"
			+ MessageKeys.validationResultsLimitPerConstraint$description + "}")
	private long validationResultsLimitPerConstraint = ShaclSailConfig.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT_DEFAULT;

}
