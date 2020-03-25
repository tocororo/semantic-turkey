package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.config.customservice.OperationDefintion;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Implementation of the {@link CustomServiceBackend} that uses SPARQL.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class SPARQLCustomServiceBackend implements CustomServiceBackend {

	public SPARQLCustomServiceBackend(SPARQLCustomServiceBackendConfiguration conf) {
	}

	@Override
	public InvocationHandler createInvocationHandler(OperationDefintion operationDefinition) {
		return new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				STServiceContext stServiceContext = (STServiceContext) proxy.getClass()
						.getDeclaredField("stServiceContext").get(proxy);
				Repository repo = STServiceContextUtils.getRepostory(stServiceContext);
				RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo, false);
				TupleQuery query = conn.prepareTupleQuery(
						operationDefinition.implementation.getConfig().get("sparql").asText());
				Parameter[] params = method.getParameters();

				for (int i = 0; i < params.length; i++) {
					query.setBinding(params[i].getName(),
							conn.getValueFactory().createLiteral(args[i].toString()));
				}

				String out = QueryResults.toString(query.evaluate(), "\n");

				return out;
			}
		};
	}

	@Override
	public boolean isWrite() {
		return false;
	}

}
