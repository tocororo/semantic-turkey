package it.uniroma2.art.semanticturkey.coda.converters;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.coda.converters.contracts.RandomIdGenerator;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.interfaces.CODAContext;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

/**
 * A converter implementing the CODA contract {@link RandomIdGenerator} by using the generator bound to the
 * extension point {@link URIGenerator}. This converter allows PEARL specifications executed within Semantic
 * Turkey (e.g. as it happens in the context of custom ranges) to generate URIs that are consistent with the
 * ones generated by Semantic Turkey itself.
 *
 */
public class STSpecificRandomIDGenerator implements RandomIdGenerator {

	public static final String CONVERTER_URI = "http://semanticturkey.uniroma2.it/coda/converters/randIdGen";

	@Autowired
	private STServiceContext stServiceContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.coda.contracts.RandomIdGenerator#produceURI(it.uniroma2.art.coda.interfaces.CODAContext
	 * , java.lang.String, java.util.Map)
	 */
	@Override
	public ARTURIResource produceURI(CODAContext ctx, String value, String xRole, Map<String, ARTNode> args)
			throws ConverterException {
		try {
			return stServiceContext.getProject().getURIGenerator().generateURI(stServiceContext, xRole, args);
		} catch (URIGenerationException e) {
			throw new ConverterException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.coda.contracts.RandomIdGenerator#produceURI(it.uniroma2.art.coda.interfaces.CODAContext
	 * , java.lang.String, java.lang.String)
	 */
	@Override
	public ARTURIResource produceURI(CODAContext ctx, String value, String xRole) throws ConverterException {
		try {
			return stServiceContext.getProject().getURIGenerator()
					.generateURI(stServiceContext, xRole, Collections.<String, ARTNode> emptyMap());
		} catch (URIGenerationException e) {
			throw new ConverterException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.uniroma2.art.coda.contracts.RandomIdGenerator#produceURI(it.uniroma2.art.coda.interfaces.CODAContext
	 * , java.lang.String)
	 */
	@Override
	public ARTURIResource produceURI(CODAContext ctx, String value) throws ConverterException {
		try {
			return stServiceContext.getProject().getURIGenerator()
					.generateURI(stServiceContext, "undetermined", Collections.<String, ARTNode> emptyMap());
		} catch (URIGenerationException e) {
			throw new ConverterException(e);
		}
	}

}
