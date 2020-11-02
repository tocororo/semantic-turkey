package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.Set;

import javax.validation.constraints.Size;

import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
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
public class EcoPortalDeployerConfiguration extends OntoPortalDeployerConfiguration implements Configuration {

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
		@Enumeration({ "AlternativeTitle", "Subtitle", "TranslatedTitle", "Other" })
		public String titleType;

	};

	@Override
	public String getShortName() {
		return "EcoPortal";
	}

	/*
	 * E’ una lista di creator con almeno un elemento. E’ sufficiente inserire solo la denominazione dei
	 * creator (creatorName) . Di seguito un esempio. { "creators":[ { "creatorName":"Mario Rossi" }, {
	 * "creatorName":"NomeOrganizzazione" } ] }
	 * 
	 * 
	 */
	@STProperty(displayName = "Creators", description = "The main researchers involved in producing the data, or the authors of the publication, in priority order")
	@Required
	@Size(min = 1)
	public Set<String> creators; // TODO: implement min lenght = 1

	/*
	 * E’ necessario specificare almeno un titolo con le relative informazioni. Esempio { "titles":[ {
	 * "title":"Alien Species Thesaurus", "lang":"en-EN", "titleType":"Other" } ] }
	 * 
	 * I campi title, lang e titleType sono obbligatori.
	 * 
	 * Suggerirei di inserire solo un titolo forzando il campo titleType al valore “Other” (evitando, quindi,
	 * di inserirlo nel form)
	 * 
	 */
	
	@STProperty(displayName = "Titles", description = "")
	@Required
	@Size(min = 1)
	public Set<Title> titles;

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

	{
		// defines the default address of the EcoPortal server
		this.apiBaseURL = "http://example.org/";
	}
}
