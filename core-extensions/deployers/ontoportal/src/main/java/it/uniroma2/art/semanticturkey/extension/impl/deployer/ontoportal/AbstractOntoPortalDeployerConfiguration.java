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

	public static final String CONTACT_PATTERN = "^\\s*(?<name>.+)\\s*\\((?<email>.+)\\s*\\)$";

	@Override
	public String getHTMLWarning() {
		return "The API key is stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see it.";
	}

	@STProperty(description = "The base URL of the OntoPortal REST API", displayName = "API Base URL")
	public String apiBaseURL;

	@STProperty(description = "A valid API key for the OntoPortal REST API. A user's API key be found on the account page of that user", displayName = "API Key")
	@Required
	public String apiKey;

	@STProperty(description = "The acronym of the ontology for which the submission is being done", displayName = "Acronym")
	@Required
	public String acronym;

	@STProperty(description = "Description of the ontology", displayName = "Description")
	@Required
	public String description;

	@STProperty(description = "Version of the ontology", displayName = "Version")
	public String version;

	@STProperty(description = "Ontology language", displayName = "Format")
	@Enumeration({ "OWL", "SKOS" })
	@Required
	public String hasOntologyLanguage;

	@STProperty(description = "Status of the ontology", displayName = "Status")
	@Enumeration({ "alpha", "beta", "production", "retired" })
	@Required
	public String status;

	@STProperty(description = "Release date. The date shall be formatted as yyyy-mm-dd (e.g. 2020-02-20)", displayName = "Release date")
	@Required
	@Pattern(regexp = "^\\d\\d\\d\\d-(0?[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$")
	public String released;

	@STProperty(description = "Contacts for the ontology. Each contact should be provided as a string matching this paattern: name (email)", displayName = "Contacts")
	@Required
	public List<@RegExp(regexp = CONTACT_PATTERN) String> contact;

	@STProperty(description = "Address of the main web page of the ontology", displayName = "Homepage")
	public String homepage; // note: it should be URL but this type is not currently supported by the UI

	@STProperty(description = "Address of a web page providing documentation for the ontology", displayName = "Documentation")
	public String documentation;

	@STProperty(description = "Address of a web page listing publications about the ontology", displayName = "Publications")
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
