package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.zthesdeserializer;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.xml.sax.SAXException;

import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LifterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.RDFLifter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.zthes.XmlReader;
import it.uniroma2.art.semanticturkey.zthes.Zthes;
import it.uniroma2.art.semanticturkey.zthes.ZthesException;
import it.uniroma2.art.semanticturkey.zthes.ZthesToRdfMapper;

/**
 * An {@link RDFLifter} that deserializes RDF data according to the Zthes format
 * 
 * @author <a href="mailto:tiziano.lorenzetti@gmail.com">Tiziano Lorenzetti</a>
 */
public class ZthesDeserializingLifter implements RDFLifter {

	@Override
	public void lift(ClosableFormattedResource sourceFormattedResource, String format,
			RDFHandler targetRDFHandler, LifterContext lifterContext) throws LiftingException, IOException {
		try {
			XmlReader xmlReader = new XmlReader();
			Zthes zThes = xmlReader.parseZThes(sourceFormattedResource.getInputStream());
			ZthesToRdfMapper mapper = new ZthesToRdfMapper(zThes, lifterContext);
			Model model = mapper.map();
			for (Statement stmt : model) {
				targetRDFHandler.handleStatement(stmt);
			}
		} catch (ZthesException | SAXException | ParserConfigurationException | URIGenerationException e) {
			throw new LiftingException(e);
		}
	}

}
