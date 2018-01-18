package it.uniroma2.art.semanticturkey.properties.core;

import it.uniroma2.art.semanticturkey.properties.ContentType;
import it.uniroma2.art.semanticturkey.properties.ContentTypeVocabulary;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesImpl;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ProjectPreferences extends STPropertiesImpl implements STProperties {

	@Override
	public String getShortName() {
		return "Semantic Turkey core project preferences";
	}

	@STProperty(description = "the column-separated ordered list of languages that will be used for rendering resources (if the resource renderer supports rendering of lexical information).")
	public String languages;

	@STProperty(description = "if true, flags will be used to represent the language of lexicalizations; if false, the character code will be shown")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean show_flags;

	@STProperty(description = "show the number of instances for each class in some class browsing UI components (such as the the class tree)")
	@ContentType(ContentTypeVocabulary.BOOLEAN)
	public boolean show_instance_numbers;

	@STProperty(description = "scheme selected for showing concepts in the SKOS concept tree")
	public String active_scheme;
}
