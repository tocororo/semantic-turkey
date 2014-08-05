package it.uniroma2.art.semanticturkey.data.access.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.TupleQueryDataAccess;

import java.util.Collection;

public class TupleQueryDataAccessSPARQLImpl implements TupleQueryDataAccess {

	private CloseableTripleQueryModel queryModel;

	public TupleQueryDataAccessSPARQLImpl(CloseableTripleQueryModel queryModel) {
		this.queryModel = queryModel;
	}
	
	@Override
	public TupleBindingsIterator retrieveInformationAbout(Collection<ARTURIResource> uriResources, String graphPattern) throws DataAccessException {
		StringBuilder sb = new StringBuilder();
		sb.append("select * where {\n");
		sb.append("{\n");
		sb.append(graphPattern);
		sb.append("}\n");
		
		sb.append("values(?resource){\n");

		// TODO: better quota management
		int remainingResouces = MAXIMUM_RESOURCE_COUNT_FOR_BATCH;

		for (ARTURIResource uriRes : uriResources) {
			if (remainingResouces == 0) {
				break;
			} else {
				remainingResouces--;
			}

			sb.append("(" + RDFNodeSerializer.toNT(uriRes) + ")\n");
		}
		
		sb.append("}\n");
		sb.append("}");
		
		TupleQuery query;
		try {
			query = queryModel.createTupleQuery(QueryLanguage.SPARQL, sb.toString(), null);
			return query.evaluate(true);
		} catch (UnsupportedQueryLanguageException | ModelAccessException | MalformedQueryException | QueryEvaluationException e) {
			throw new DataAccessException(e);
		}
	}
}
