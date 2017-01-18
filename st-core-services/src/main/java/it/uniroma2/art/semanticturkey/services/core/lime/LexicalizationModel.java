package it.uniroma2.art.semanticturkey.services.core.lime;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public interface LexicalizationModel {

	Map<String, LanguageStatistics> computeStatistics(RepositoryConnection repConn,
			IRI[] lexicalizationSetGraphs);

	String getName();

	IRI getIRI();

}