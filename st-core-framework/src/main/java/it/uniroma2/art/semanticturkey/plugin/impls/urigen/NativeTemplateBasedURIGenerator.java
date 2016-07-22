package it.uniroma2.art.semanticturkey.plugin.impls.urigen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.coda.interfaces.CODAContext;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

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

		convProps = new Properties(conf.getProperties());

		propsMap = new HashMap<>();
		propsMap.put(TemplateBasedRandomIdGenerator.CONVERTER_URI, convProps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator#generateURI(it.uniroma2.art.semanticturkey.
	 * services.STServiceContext, java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResource generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, ARTNode> args) throws URIGenerationException {
		try {
			String randomCode = ProjectManager.getProjectProperty(stServiceContext.getProject().getName(), TemplateBasedRandomIdGenerator.PARAM_URI_RND_CODE_GENERATOR);
			
			if (randomCode != null) {
				convProps.setProperty(TemplateBasedRandomIdGenerator.PARAM_URI_RND_CODE_GENERATOR, randomCode);
			}
		} catch (IOException | InvalidProjectNameException | ProjectInexistentException e) {
		}
		CODAContext ctx = new CODAContext(stServiceContext.getProject().getOntModel(), null, propsMap);
		return converter.produceURI(ctx, null, xRole, args);
	}
}
