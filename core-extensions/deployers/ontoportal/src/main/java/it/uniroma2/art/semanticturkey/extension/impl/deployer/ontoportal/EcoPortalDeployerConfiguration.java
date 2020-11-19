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

		@Override
		public String getShortName() {
			return "Title";
		}

		@STProperty(displayName = "Title", description = "A name or title by which a resource is known. May be the title of a dataset or the name of a piece of software")
		@Required
		@LanguageTaggedString
		public Literal title;

		@STProperty(displayName = "Title type", description = "The type of title")
		@Required
		@Enumeration({ "AlternativeTitle", "Subtitle", "TranslatedTitle", "Other" })
		public String titleType = "Other";

	};

	@Override
	public String getShortName() {
		return "EcoPortal";
	}

	@STProperty(displayName = "Creators", description = "The main researchers involved in producing the data, or the authors of the publication, in priority order")
	@Required
	public List<String> creators;

	@STProperty(displayName = "Titles", description = "")
	@Required
	@Valid
	public List<Title> titles;

	@STProperty(displayName = "Publisher", description = "The name of the entitythat holds, archives, publishes prints, distributes, releases, issues, or produces the resource")
	@Required
	public String publisher;

	@STProperty(displayName = "Publication year", description = "The year when the data was or will be made publicly available")
	@Required
	public Integer publicationYear;

	@STProperty(displayName = "Resource type", description = "A description of the resource. The format is open, but the preferred format is a single term of some detail. ")
	@Required
	@Enumeration(value = { "Authority File", "Controlled Vocabulary", "Glossary", "Ontology",
			"Thesaurus" }, open = true)
	public String resourceType;

	@STProperty(displayName = "Resource type general", description = "The general type of a resource")
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
