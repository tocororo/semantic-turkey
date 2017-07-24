package it.uniroma2.art.semanticturkey.tx;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;

/**
 * A {@code FactoryBean} that is used to instantiate the {@link RDF4JRepositoryTransactionManager} managing
 * the repository associated with the current project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RDF4JRepositoryTransactionManagerFactoryBean
		implements FactoryBean<RDF4JRepositoryTransactionManager> {

	@Autowired
	private STServiceContext stContext;

	@Override
	public RDF4JRepositoryTransactionManager getObject() throws Exception {
		return stContext.getProject()
				.getRepositoryTransactionManager(STServiceContextUtils.getRepostoryId(stContext));
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
