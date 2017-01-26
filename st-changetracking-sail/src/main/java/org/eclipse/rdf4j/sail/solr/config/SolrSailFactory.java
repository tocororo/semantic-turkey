package org.eclipse.rdf4j.sail.solr.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public class SolrSailFactory implements SailFactory {
	public static final String SAIL_TYPE = "http://semanticturkey.uniroma2.it/sail/null";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return null;
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		return null;
	}

}
