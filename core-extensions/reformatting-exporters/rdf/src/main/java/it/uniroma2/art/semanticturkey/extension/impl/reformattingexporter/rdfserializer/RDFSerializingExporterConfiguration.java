package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A configuration for the {@link RDFSerializingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RDFSerializingExporterConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporterConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String prettyPrint$description = keyBase + ".prettyPrint.description";
		public static final String prettyPrint$displayName = keyBase + ".prettyPrint.displayName";
		public static final String inlineBlankNodes$description = keyBase + ".inlineBlankNodes.description";
		public static final String inlineBlankNodes$displayName = keyBase + ".inlineBlankNodes.displayName";
		public static final String xsdStringToPlainLiteral$description = keyBase + ".xsdStringToPlainLiteral.description";
		public static final String xsdStringToPlainLiteral$displayName = keyBase + ".xsdStringToPlainLiteral.displayName";
		public static final String rdfLangStringToLangLiteral$description = keyBase + ".rdfLangStringToLangLiteral.description";
		public static final String rdfLangStringToLangLiteral$displayName = keyBase + ".rdfLangStringToLangLiteral.displayName";
		public static final String baseDirective$description = keyBase + ".baseDirective.description";
		public static final String baseDirective$displayName = keyBase + ".baseDirective.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.prettyPrint$description + "}", displayName = "{" + MessageKeys.prettyPrint$displayName + "}")
	public Boolean prettyPrint;

	@STProperty(description = "{" + MessageKeys.inlineBlankNodes$description + "}", displayName = "{" + MessageKeys.inlineBlankNodes$displayName + "}")
	public Boolean inlineBlankNodes;

	@STProperty(description = "{" + MessageKeys.xsdStringToPlainLiteral$description + "}", displayName = "{" + MessageKeys.xsdStringToPlainLiteral$displayName + "}")
	public Boolean xsdStringToPlainLiteral;

	@STProperty(description = "{" + MessageKeys.rdfLangStringToLangLiteral$description + "}", displayName = "{" + MessageKeys.rdfLangStringToLangLiteral$displayName + "}")
	public Boolean rdfLangStringToLangLiteral;

	@STProperty(description = "{" + MessageKeys.baseDirective$description + "}", displayName = "{" + MessageKeys.baseDirective$displayName + "}")
	public Boolean baseDirective;

}
