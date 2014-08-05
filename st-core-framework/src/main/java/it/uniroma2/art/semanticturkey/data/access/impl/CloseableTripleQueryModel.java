package it.uniroma2.art.semanticturkey.data.access.impl;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.models.TripleQueryModel;
import it.uniroma2.art.owlart.models.TripleQueryModelHTTPConnection;
import it.uniroma2.art.owlart.query.BooleanQuery;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.Query;
import it.uniroma2.art.owlart.query.QueryLanguage;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.query.Update;

public class CloseableTripleQueryModel implements TripleQueryModel {

	private TripleQueryModel queryModel;
	private boolean mustDisconnect;

	public CloseableTripleQueryModel(TripleQueryModel queryModel) {
		this.queryModel = queryModel;
		this.mustDisconnect = false;
	}

	public CloseableTripleQueryModel(TripleQueryModelHTTPConnection queryConnect) {
		this.queryModel = queryConnect;
		this.mustDisconnect = true;
	}

	@Override
	public Query createQuery(QueryLanguage ql, String query, String baseURI)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		return queryModel.createQuery(ql, query, baseURI);
	}

	@Override
	public BooleanQuery createBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		return queryModel.createBooleanQuery(ql, query, baseURI);
	}

	@Override
	public GraphQuery createGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		return queryModel.createGraphQuery(ql, query, baseURI);
	}

	@Override
	public TupleQuery createTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		return queryModel.createTupleQuery(ql, query, baseURI);
	}

	@Override
	public Update createUpdate(QueryLanguage ql, String query, String baseURI)
			throws UnsupportedQueryLanguageException, ModelAccessException, MalformedQueryException {
		return queryModel.createUpdate(ql, query, baseURI);
	}

	public void close() throws ModelAccessException {
		if (mustDisconnect) {
			((TripleQueryModelHTTPConnection) queryModel).disconnect();
		}
	}

}
