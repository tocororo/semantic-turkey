package it.uniroma2.art.semanticturkey.data.access.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.utilities.RDFIterators;
import it.uniroma2.art.semanticturkey.data.access.DataAccessException;
import it.uniroma2.art.semanticturkey.data.access.DescribeDataAccess;

import java.util.Collection;
import java.util.Set;

public class DescribeDataAccessSPARQLImpl implements DescribeDataAccess {

	private CloseableTripleQueryModel queryModel;

	public DescribeDataAccessSPARQLImpl(CloseableTripleQueryModel queryModel) {
		this.queryModel = queryModel;
	}

	@Override
	public Collection<ARTStatement> retrieveInformationAbout(Collection<ARTURIResource> uriResources)
			throws DataAccessException {
		StringBuilder sb = new StringBuilder();
		sb.append("describe ?resource where {\n");
		sb.append("   values(?resource){\n");

		// TODO: better quota management
		int remainingResouces = MAXIMUM_RESOURCE_COUNT_FOR_BATCH;
		for (ARTURIResource uriRes : uriResources) {
			if (remainingResouces == 0) {
				break;
			} else {
				remainingResouces--;
			}
			sb.append("      (" + RDFNodeSerializer.toNT(uriRes) + ")\n");
		}

		sb.append("   }\n");
		sb.append("}");

		try {
			GraphQuery q = queryModel.createGraphQuery(QueryLanguage.SPARQL, sb.toString(), null);
			Set<ARTStatement> result = RDFIterators.getSetFromIterator(q.evaluate(true));
			
			queryModel.close();
			
			return result;
			
		} catch (ModelAccessException | UnsupportedQueryLanguageException | MalformedQueryException | QueryEvaluationException e) {
			throw new DataAccessException(e);
		}
	}
}
