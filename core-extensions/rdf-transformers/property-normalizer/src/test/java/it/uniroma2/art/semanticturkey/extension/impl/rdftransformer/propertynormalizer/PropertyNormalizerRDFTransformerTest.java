package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import org.junit.Test;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.AbstractRDFTransformerTest;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer.PropertyNormalizerTransformer;

/**
 * Test class for the {@link PropertyNormalizerTransformer} implementation of {@link RDFTransformer}.
 * 
 */
public class PropertyNormalizerRDFTransformerTest extends AbstractRDFTransformerTest {

	@Test
	public void test1() throws Exception {
		executeTest();
	}

	@Override
	protected boolean isPrintEnabled(String testName) {
		return false;
	}

}
