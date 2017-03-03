package it.uniroma2.art.semanticturkey.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;

import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.rdf4jimpl.RDF4JARTResourceFactory;
import it.uniroma2.art.owlart.rdf4jimpl.io.RDFFormatConverter;
import it.uniroma2.art.owlart.rdf4jimpl.models.RDFModelRDF4J;

public abstract class RDF4JMigrationUtils {
	private static ValueFactory vf = SimpleValueFactory.getInstance();
	private static RDF4JARTResourceFactory fact = new RDF4JARTResourceFactory(vf);

	public static IRI convert2rdf4j(ARTURIResource resource) {
		return vf.createIRI(resource.getNominalValue());
	}

	public static Resource convert2rdf4j(ARTResource resource) {
		String nominalValue = resource.getNominalValue();
		return resource.isBlank() ? vf.createBNode(nominalValue) : vf.createIRI(nominalValue);
	}

	public static Map<String, Value> convert2rdf4j(Map<String, ARTNode> map) {
		Map<String, Value> rv = new HashMap<>();

		for (Map.Entry<String, ARTNode> entry : map.entrySet()) {
			rv.put(entry.getKey(), fact.aRTNode2RDF4JValue(entry.getValue()));
		}

		return rv;
	}

	public static ARTURIResource convert2art(IRI resource) {
		return fact.rdf4jURI2ARTURIResource(resource);
	}

	public static ARTResource convert2art(Resource resource) {
		return fact.rdf4jResource2ARTResource(resource);
	}

	public static ARTNode convert2art(Value value) {
		return fact.rdf4jValue2ARTNode(value);
	}

	public static Map<String, ARTNode> convert2art(Map<String, Value> map) {
		Map<String, ARTNode> rv = new HashMap<>();

		for (Map.Entry<String, Value> entry : map.entrySet()) {
			rv.put(entry.getKey(), fact.rdf4jValue2ARTNode(entry.getValue()));
		}

		return rv;
	}

	public static RepositoryConnection extractRDF4JConnection(RDFModel rdfModel) {
		return ((RDFModelRDF4J) rdfModel).getRDF4JRepositoryConnection();
	}

	public static it.uniroma2.art.owlart.io.RDFFormat convert2art(RDFFormat rdfFormat) {
		return it.uniroma2.art.owlart.io.RDFFormat
				.guessRDFFormatFromFile(new File("temp." + rdfFormat.getDefaultFileExtension()));
	}

	public static RDFFormat convert2rdf4j(it.uniroma2.art.owlart.io.RDFFormat rdfFormat) {
		try {
			return RDFFormatConverter.convert(rdfFormat);
		} catch (UnsupportedRDFFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
