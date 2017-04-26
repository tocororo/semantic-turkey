package it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge;

import org.eclipse.rdf4j.sail.config.SailImplConfig;

import com.ontotext.trree.free.GraphDBFreeSailFactory;

import it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge.conf.GraphDBFreeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.plugin.extpts.SailConfigurer;

public class GraphDBFreeSailConfigurer implements SailConfigurer {
	private GraphDBFreeSailConfigurerConfiguration config;

	public GraphDBFreeSailConfigurer(GraphDBFreeSailConfigurerConfiguration config) {
		this.config = config;
	}

	@Override
	public SailImplConfig buildSailConfig() {
		SailImplConfig sailImplConfig = new GraphDBFreeSailFactory().getConfig();
		System.out.println("@@@" + sailImplConfig.getClass());

		return sailImplConfig;
	}
}
