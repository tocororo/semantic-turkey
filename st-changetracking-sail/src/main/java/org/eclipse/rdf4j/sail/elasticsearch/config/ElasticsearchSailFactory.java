package org.eclipse.rdf4j.sail.elasticsearch.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public class ElasticsearchSailFactory implements SailFactory {
	public static final String SAIL_TYPE = "http://semanticturkey.uniroma2.it/sail/null2";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		// TODO Auto-generated method stub
		return null;
	}

}
