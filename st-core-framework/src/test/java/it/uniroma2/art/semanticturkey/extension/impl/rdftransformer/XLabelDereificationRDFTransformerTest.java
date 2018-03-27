package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer;

import org.junit.Test;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification.XLabelDereificationRDFTransformer;

/**
 * Test class for the {@link XLabelDereificationRDFTransformer} implementation of {@link RDFTransformer}.
 * 
 */
public class XLabelDereificationRDFTransformerTest extends AbstractRDFTransformerTest {

	@Test
	public void test1() throws Exception {
		executeTest();
	}
	
	@Test
	public void test2() throws Exception {
		executeTest();
	}
	
	@Test
	public void test3() throws Exception {
		executeTest();
	}

	@Test
	public void test4() throws Exception {
		executeTest();
	}
	
	@Test
	public void test5() throws Exception {
		executeTest();
	}

	
	@Override
	protected boolean isPrintEnabled(String testName) {
		// TODO Auto-generated method stub
		//return super.isPrintEnabled(testName);
		return false;
	}
}
