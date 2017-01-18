package it.uniroma2.art.semanticturkey.services.core.lime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class LexicalizationModelRegistry {
	private Map<IRI, LexicalizationModel> iri2lexicalizationModel = new HashMap<>();
	
	{
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		iri2lexicalizationModel.put(vf.createIRI("http://www.w3.org/2000/01/rdf-schema"), new RDFSLexicalizationModel());
		iri2lexicalizationModel.put(vf.createIRI("http://www.w3.org/2004/02/skos/core"), new SKOSLexicalizationModel());
		iri2lexicalizationModel.put(vf.createIRI("http://www.w3.org/2008/05/skos-xl"), new SKOSXLLexicalizationModel());
	}
	
	public Optional<LexicalizationModel> getLexicalizationModel(IRI identifier) {
		return Optional.ofNullable(iri2lexicalizationModel.get(identifier));
	}
}
