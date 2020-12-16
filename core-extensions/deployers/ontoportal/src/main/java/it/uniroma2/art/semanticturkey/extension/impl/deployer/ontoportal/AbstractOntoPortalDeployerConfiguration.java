package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.List;

import javax.validation.constraints.Pattern;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.RegExp;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Abstract base class of configuration classes for the {@link OntoPortalDeployerFactory}. The parameter
 * {@link #apiBaseURL} is optional, as long as concrete subclasses assume a sensible default or tighten its
 * definition making it mandatory.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class AbstractOntoPortalDeployerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.AbstractOntoPortalDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String apiBaseURL$description = keyBase + ".apiBaseURL.description";
		public static final String apiBaseURL$displayName = keyBase + ".apiBaseURL.displayName";
		public static final String apiKey$description = keyBase + ".apiKey.description";
		public static final String apiKey$displayName = keyBase + ".apiKey.displayName";
		public static final String acronym$description = keyBase + ".acronym.description";
		public static final String acronym$displayName = keyBase + ".acronym.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String version$description = keyBase + ".version.description";
		public static final String version$displayName = keyBase + ".version.displayName";
		public static final String hasOntologyLanguage$description = keyBase + ".hasOntologyLanguage.description";
		public static final String hasOntologyLanguage$displayName = keyBase + ".hasOntologyLanguage.displayName";
		public static final String status$description = keyBase + ".status.description";
		public static final String status$displayName = keyBase + ".status.displayName";
		public static final String released$description = keyBase + ".released.description";
		public static final String released$displayName = keyBase + ".released.displayName";
		public static final String contact$description = keyBase + ".contact.description";
		public static final String contact$displayName = keyBase + ".contact.displayName";
		public static final String homepage$description = keyBase + ".homepage.description";
		public static final String homepage$displayName = keyBase + ".homepage.displayName";
		public static final String documentation$description = keyBase + ".documentation.description";
		public static final String documentation$displayName = keyBase + ".documentation.displayName";
		public static final String publication$description = keyBase + ".publication.description";
		public static final String publication$displayName = keyBase + ".publication.displayName";
	}

	public static final String CONTACT_PATTERN = "^\\s*(?<name>.+)\\s*\\((?<email>.+)\\s*\\)$";

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.apiBaseURL$description+ "}", displayName = "{" + MessageKeys.apiBaseURL$displayName + "}")
	public String apiBaseURL;

	@STProperty(description = "{" + MessageKeys.apiKey$description+ "}", displayName = "{" + MessageKeys.apiKey$displayName + "}")
	@Required
	public String apiKey;

	@STProperty(description = "{" + MessageKeys.acronym$description+ "}", displayName = "{" + MessageKeys.acronym$displayName + "}")
	@Required
	public String acronym;

	@STProperty(description = "{" + MessageKeys.description$description+ "}", displayName = "{" + MessageKeys.description$displayName + "}")
	@Required
	public String description;

	@STProperty(description = "{" + MessageKeys.version$description+ "}", displayName = "{" + MessageKeys.version$displayName + "}")
	public String version;

	@STProperty(description = "{" + MessageKeys.hasOntologyLanguage$description+ "}", displayName = "{" + MessageKeys.hasOntologyLanguage$displayName + "}")
	@Enumeration({ "OWL", "SKOS" })
	@Required
	public String hasOntologyLanguage;

	@STProperty(description = "{" + MessageKeys.status$description+ "}", displayName = "{" + MessageKeys.status$displayName + "}")
	@Enumeration({ "alpha", "beta", "production", "retired" })
	@Required
	public String status;

	@STProperty(description = "{" + MessageKeys.released$description+ "}", displayName = "{" + MessageKeys.released$displayName + "}")
	@Required
	@Pattern(regexp = "^\\d\\d\\d\\d-(0?[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$")
	public String released;

	@STProperty(description = "{" + MessageKeys.contact$description+ "}", displayName = "{" + MessageKeys.contact$displayName + "}")
	@Required
	public List<@RegExp(regexp = CONTACT_PATTERN) String> contact;

	@STProperty(description = "{" + MessageKeys.homepage$description+ "}", displayName = "{" + MessageKeys.homepage$displayName + "}")
	public String homepage; // note: it should be URL but this type is not currently supported by the UI

	@STProperty(description = "{" + MessageKeys.documentation$description+ "}", displayName = "{" + MessageKeys.documentation$displayName + "}")
	public String documentation;

	@STProperty(description = "{" + MessageKeys.publication$description+ "}", displayName = "{" + MessageKeys.publication$displayName + "}")
	public String publication;

	// public static class Contact implements STProperties {
	//
	// @Override
	// public String getShortName() {
	// return "Contact description";
	// }
	//
	// @STProperty(description = "the name of a contact", displayName = "Name")
	// @Required
	// public String name;
	//
	// @STProperty(description = "the email of a contact", displayName = "Email")
	// @Required
	// public String email;
	//
	// }

}
