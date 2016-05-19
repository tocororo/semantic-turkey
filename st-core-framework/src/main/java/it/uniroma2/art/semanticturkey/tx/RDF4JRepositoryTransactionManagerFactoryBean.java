package it.uniroma2.art.semanticturkey.tx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openrdf.repository.Repository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceContext;

/**
 * A {@code FactoryBean} that is used to instantiate the {@link RDF4JRepositoryTransactionManager} managing
 * the repository associated with the current project.
 * 
 * TODO: replace this implementation with something that is aware of the act of closing a project
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RDF4JRepositoryTransactionManagerFactoryBean
		implements FactoryBean<RDF4JRepositoryTransactionManager> {

	@Autowired
	private STServiceContext stContext;

	// TODO move to project
	private static Map<String, RDF4JRepositoryTransactionManager> transactionManagers = new ConcurrentHashMap<>();

	@Override
	public RDF4JRepositoryTransactionManager getObject() throws Exception {
		final String projectName = stContext.getProject().getName();
		final Repository repository = stContext.getProject().getRepository();

		return transactionManagers.computeIfAbsent(projectName,
				pn -> new RDF4JRepositoryTransactionManager(repository));
	}

	@Override
	public Class<?> getObjectType() {
		return RDF4JRepositoryTransactionManager.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
