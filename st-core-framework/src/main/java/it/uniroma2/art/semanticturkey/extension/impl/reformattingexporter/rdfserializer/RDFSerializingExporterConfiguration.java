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

	@Override
	public String getShortName() {
		return "RDF Serializing Exporter";
	}

	@STProperty(displayName = "pretty print", description = "Tells whether pretty printing is preferred")
	public Boolean prettyPrint;

	@STProperty(displayName = "inline blank nodes", description = "Tells whether to use blank node property lists, collections, and anonymous nodes instead of blank node labels."
			+ "This settings requires that all triples are first loaded in memory, and should not be checked for large datasets")
	public Boolean inlineBlankNodes;

	@STProperty(displayName = "xsd:string to plain literal", description = "Tells whether the serializer should remove the xsd:string datatype from literals and represent them as RDF-1.0 Plain Literals")
	public Boolean xsdStringToPlainLiteral;

	@STProperty(displayName = "rdf:langString to language tagged literal", description = "Tells whether the serializer should omit the rdf:langString datatype from language literals when serializing them")
	public Boolean rdfLangStringToLangLiteral;

	@STProperty(displayName = "base directive", description = "Tells whether the serializer should include a base directive")
	public Boolean baseDirective;

}
