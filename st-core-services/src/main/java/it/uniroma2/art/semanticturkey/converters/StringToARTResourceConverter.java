package it.uniroma2.art.semanticturkey.converters;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.semanticturkey.converters.impl.STSpecificNodeChecks;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.utilities.PrefixMappingRDF4JImpl;

@Deprecated
public class StringToARTResourceConverter implements Converter<String, ARTResource> {

	@Autowired
	private STServiceContext serviceContext;

	@Override
	public ARTResource convert(String NTTerm) {
		Repository repo = STServiceContextUtils.getRepostory(serviceContext);
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo);
		try {
			try {
				if (NTTerm.equals("ANY"))
					return NodeFilters.ANY;
				if (NTTerm.equals("DEFAULT"))
					return NodeFilters.MAINGRAPH;

				ARTResource resource = RDFNodeSerializer.createResource(new PrefixMappingRDF4JImpl(conn), NTTerm);
				STSpecificNodeChecks.checkURIResourceConstraints(resource);
				return resource;
			} catch (ModelAccessException e) {
				throw new RuntimeException(e);
			}
		} finally {
			RDF4JRepositoryUtils.releaseConnection(conn, repo);
		}
	}

}
