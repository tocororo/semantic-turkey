package it.uniroma2.art.semanticturkey.graphdb.rdf4jbridge;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ontotext.trree.config.OWLIMChildSailFactory;
import com.ontotext.trree.config.OWLIMParentSailFactory;
import com.ontotext.trree.config.OWLIMSailFactory;
import com.ontotext.trree.free.GraphDBFreeRepositoryFactory;
import com.ontotext.trree.free.GraphDBFreeSailFactory;
import com.ontotext.trree.monitorRepository.MonitorRepositoryFactory;
import com.ontotext.trree.plugin.geo.Distance;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.extpts.SailConfigurer;

public class GraphDBRDF4JBridgeActivator implements BundleActivator {

	private Distance distanceFunction;
	private MonitorRepositoryFactory monitorRepositoryFactory;
	private GraphDBFreeRepositoryFactory graphDBFreeRepositoryFactory;
	private OWLIMSailFactory owlimSailFactory;
	private OWLIMParentSailFactory owlimParentSailFactory;
	private OWLIMChildSailFactory owlimChildSailFactory;
	private GraphDBFreeSailFactory graphDBFreeSailFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		FunctionRegistry fuctionRegistry = FunctionRegistry.getInstance();
		distanceFunction = new com.ontotext.trree.plugin.geo.Distance();
		fuctionRegistry.add(distanceFunction);
		
		RepositoryRegistry repositoryRegistry = RepositoryRegistry.getInstance();
		monitorRepositoryFactory = new MonitorRepositoryFactory();
		graphDBFreeRepositoryFactory = new GraphDBFreeRepositoryFactory();
		repositoryRegistry.add(monitorRepositoryFactory);
		repositoryRegistry.add(graphDBFreeRepositoryFactory);
		
		SailRegistry sailRegistry = SailRegistry.getInstance();
		owlimSailFactory = new OWLIMSailFactory();
		owlimParentSailFactory = new OWLIMParentSailFactory();
		owlimChildSailFactory = new OWLIMChildSailFactory();
		graphDBFreeSailFactory = new GraphDBFreeSailFactory();
		sailRegistry.add(owlimSailFactory);
		sailRegistry.add(owlimParentSailFactory);
		sailRegistry.add(owlimChildSailFactory);
		sailRegistry.add(graphDBFreeSailFactory);
		
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("it.uniroma2.art.semanticturkey.extensionpoint", SailConfigurer.class.getName());
		context.registerService(PluginFactory.class, new GraphDBSailConfigurerFactory(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		FunctionRegistry fuctionRegistry = FunctionRegistry.getInstance();
		fuctionRegistry.remove(distanceFunction);	
		
		RepositoryRegistry repositoryRegistry = RepositoryRegistry.getInstance();
		repositoryRegistry.remove(monitorRepositoryFactory);
		repositoryRegistry.remove(graphDBFreeRepositoryFactory);

		SailRegistry sailRegistry = SailRegistry.getInstance();
		sailRegistry.remove(owlimSailFactory);
		sailRegistry.remove(owlimParentSailFactory);
		sailRegistry.remove(owlimChildSailFactory);
		sailRegistry.remove(graphDBFreeSailFactory);
	}

}
