package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.OWLArtModelFactory;
import it.uniroma2.art.semanticturkey.data.access.impl.CloseableTripleQueryModel;
import it.uniroma2.art.semanticturkey.data.access.impl.DescribeDataAccessSPARQLImpl;
import it.uniroma2.art.semanticturkey.data.access.impl.PropertyPatternDataAccessDereferencingImpl;
import it.uniroma2.art.semanticturkey.data.access.impl.PropertyPatternDataAccessSPARQLImpl;
import it.uniroma2.art.semanticturkey.data.access.impl.TupleQueryDataAccessSPARQLImpl;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;

public class DataAccessFactory {
	public static DescribeDataAccess createDescribeDataAccess(Project<?> project) {
		CloseableTripleQueryModel queryModel = new CloseableTripleQueryModel(project.getOWLModel());
		return new DescribeDataAccessSPARQLImpl(queryModel);
	}

	public static TupleQueryDataAccess createTupleQueryDataAccess(Project<?> project) {
		CloseableTripleQueryModel queryModel = new CloseableTripleQueryModel(project.getOWLModel());
		return new TupleQueryDataAccessSPARQLImpl(queryModel);
	}

	public static PropertyPatternDataAccess createPropertyPatternDataAccess(Project<?> project) {
		CloseableTripleQueryModel queryModel = new CloseableTripleQueryModel(project.getOWLModel());
		return new PropertyPatternDataAccessSPARQLImpl(queryModel);
	}

	public static PropertyPatternDataAccess createPropertyPatternDataAccess(Project<?> project,
			DatasetMetadata meta) throws DataAccessException {
		String sparqlEndpoint = meta.getSparqlEndpoint();

		try {
			ModelFactory<?> fact = OWLArtModelFactory.createModelFactory(getCurrentModelFactory(project));

			if (sparqlEndpoint != null) {
				CloseableTripleQueryModel queryModel = new CloseableTripleQueryModel(fact.loadTripleQueryHTTPConnection(sparqlEndpoint));
				return new PropertyPatternDataAccessSPARQLImpl(queryModel);
			} else if (meta.isDereferenceable()){
				return new PropertyPatternDataAccessDereferencingImpl(fact.loadLinkedDataResolver());
			} else {
				throw new DataAccessException("Dataset not accessible");
			}
		} catch (UnavailableResourceException | ProjectInconsistentException | ModelCreationException e) {
			throw new DataAccessException(e);
		}

	}

	private static ModelFactory<?> getCurrentModelFactory(Project<?> project)
			throws UnavailableResourceException, ProjectInconsistentException {
		return PluginManager.getOntManagerImpl(project.getOntologyManagerImplID()).createModelFactory();
	}

}