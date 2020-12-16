package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link OntoPortalDeployerFactory} targeting
 * <a href="http://ecoportal.lifewatchitaly.eu/">EcoPortal</a>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see <a href="https://doi.org/10.14454/7xq3-zf69">Version 4.3. DataCite e.V.</a>
 */
public class EcoPortalDeployerConfiguration extends AbstractOntoPortalDeployerConfiguration
		implements Configuration {

	/**
	 * A structure describing a title of a resource
	 * 
	 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
	 *
	 */
	public static class Title implements STProperties {
		
		public static class MessageKeys {
			public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration.Title";

			public static final String shortName = keyBase + ".shortName";
			public static final String title$description = keyBase + ".title.description";
			public static final String title$displayName = keyBase + ".title.displayName";
			public static final String titleType$description = keyBase + ".titleType.description";
			public static final String titleType$displayName = keyBase + ".titleType.displayName";
		}

		@Override
		public String getShortName() {
			return "{" + MessageKeys.shortName + "}";
		}

		@STProperty(displayName = "{" + MessageKeys.title$displayName + "}", description = "{" + MessageKeys.title$description + "}")
		@Required
		@LanguageTaggedString
		public Literal title;

		@STProperty(displayName = "{" + MessageKeys.titleType$displayName + "}", description = "{" + MessageKeys.titleType$description + "}")
		@Required
		@Enumeration({ "AlternativeTitle", "Subtitle", "TranslatedTitle", "Other" })
		public String titleType = "Other";

	};
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.EcoPortalDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String creators$description = keyBase + ".creators.description";
		public static final String creators$displayName = keyBase + ".creators.displayName";
		public static final String titles$description = keyBase + ".titles.description";
		public static final String titles$displayName = keyBase + ".titles.displayName";
		public static final String publisher$description = keyBase + ".publisher.description";
		public static final String publisher$displayName = keyBase + ".publisher.displayName";
		public static final String publicationYear$description = keyBase + ".publicationYear.description";
		public static final String publicationYear$displayName = keyBase + ".publicationYear.displayName";
		public static final String resourceType$description = keyBase + ".resourceType.description";
		public static final String resourceType$displayName = keyBase + ".resourceType.displayName";
		public static final String resourceTypeGeneral$description = keyBase + ".resourceTypeGeneral.description";
		public static final String resourceTypeGeneral$displayName = keyBase + ".resourceTypeGeneral.displayName";
	}


	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(displayName = "{" + MessageKeys.creators$displayName + "}", description = "{" + MessageKeys.creators$description + "}")
	@Required
	public List<String> creators;

	@STProperty(displayName = "{" + MessageKeys.titles$displayName + "}", description = "")
	@Required
	@Valid
	public List<Title> titles;

	@STProperty(displayName = "{" + MessageKeys.publisher$displayName + "}", description = "{" + MessageKeys.publisher$description + "}")
	@Required
	public String publisher;

	@STProperty(displayName = "{" + MessageKeys.publicationYear$displayName + "}", description = "{" + MessageKeys.publicationYear$description + "}")
	@Required
	public Integer publicationYear;

	@STProperty(displayName = "{" + MessageKeys.resourceType$displayName + "}", description = "{" + MessageKeys.resourceType$description + "}")
	@Required
	@Enumeration(value = { "Authority File", "Controlled Vocabulary", "Glossary", "Ontology",
			"Thesaurus" }, open = true)
	public String resourceType;

	@STProperty(displayName = "{" + MessageKeys.resourceTypeGeneral$displayName + "}", description = "{" + MessageKeys.resourceTypeGeneral$description + "}")
	@Enumeration({ "Dataset", "Other" })
	@Required
	public String resourceTypeGeneral = "Dataset";

	/*
	 * Overrides #isRequiredProperty(String) to tighten the definition of the property "publication", which is
	 * optional in BioPortal but mandatory in EcoPortal
	 */
	@Override
	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		if (Objects.equals(parID, "publication")) {
			return true;
		} else {
			return super.isRequiredProperty(parID);
		}
	}

	/*
	@formatter:off
	
// main method to test validation
	
	public static void main(String[] args) {
		EcoPortalDeployerConfiguration conf = new EcoPortalDeployerConfiguration();
		conf.acronym = "TEST";
//		conf.apiKey = "xyz";
		conf.contact = Collections.singleton("Mario (mario@example.org)");
		conf.creators = Collections.singleton("Luca (luca@example.org)");
		conf.description = "descr";
		conf.documentation = "http://example.org/";
		conf.hasOntologyLanguage = "SKOS2";
		conf.status = "alpha";
		conf.publication = "http://example.org/";
		conf.publicationYear = 2020;
		conf.publisher = "Pub";
		conf.released = "2020-11-13";
		conf.resourceType = "Other";
		conf.resourceTypeGeneral = "Other";
		Title title = new Title();
		title.title = null; // SimpleValueFactory.getInstance().createLiteral("hello");
		conf.titles = Sets.newLinkedHashSet();
		conf.titles.addAll(Arrays.asList(title, null));
		
		STPropertiesChecker checker = STPropertiesChecker.getModelConfigurationChecker(conf);
		checker.isValid();
		System.out.println(checker.getErrorMessage());
	}
	
		@formatter:on
	*/
}
