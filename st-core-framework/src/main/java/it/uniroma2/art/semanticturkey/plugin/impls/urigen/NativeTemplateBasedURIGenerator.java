package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.interfaces.CODAContext;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;

/**
 * Implementation of the {@link URIGenerator} extension point based on templates.
 * 
 */
public class NativeTemplateBasedURIGenerator implements URIGenerator {

	private TemplateBasedRandomIdGenerator converter;

	private Map<String, Properties> propsMap;

	private Properties convProps;

	public NativeTemplateBasedURIGenerator(NativeTemplateBasedURIGeneratorConfiguration conf) {
		this.converter = new TemplateBasedRandomIdGenerator();

		convProps = new Properties(conf.getBackingProperties());

		propsMap = new HashMap<>();
		propsMap.put(TemplateBasedRandomIdGenerator.CONVERTER_URI, convProps);
	}

	@Override
	public IRI generateIRI(STServiceContext stServiceContext, String xRole, Map<String, Value> args)
			throws URIGenerationException {
		try {
			String randomCode = ProjectManager.getProjectProperty(stServiceContext.getProject().getName(),
					TemplateBasedRandomIdGenerator.PARAM_URI_RND_CODE_GENERATOR);

			if (randomCode != null) {
				convProps.setProperty(TemplateBasedRandomIdGenerator.PARAM_URI_RND_CODE_GENERATOR,
						randomCode);
			}
		} catch (IOException | InvalidProjectNameException | ProjectInexistentException e) {
		}
		Repository repo = stServiceContext.getProject().getRepository();
		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo);
		try {

			CODAContext ctx = new CODAContext(conn, propsMap);
			try {
				IRI resource = converter.produceURI(ctx, null, xRole,
						args);
				return resource;
			} catch (ConverterException e) {
				throw new URIGenerationException(e);
			}
		} finally {
			RDF4JRepositoryUtils.releaseConnection(conn, repo);
		}
	}
}
