package it.uniroma2.art.semanticturkey.services.core.lime;

import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class SemanticModel {

	public DatasetStatistics computeStatistics(RepositoryConnection repConn, IRI referenceDataset,
			IRI[] semanticModelGraphs) {
		DatasetStatistics datasetStatistics = new DatasetStatistics();
		
		BigInteger triples = BigInteger.valueOf(repConn.size(semanticModelGraphs));
		
		datasetStatistics.setTriples(triples);
		
		return datasetStatistics;
	}
	
	public IRI getIRI() {
		return SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2002/07/owl");
	}

}
