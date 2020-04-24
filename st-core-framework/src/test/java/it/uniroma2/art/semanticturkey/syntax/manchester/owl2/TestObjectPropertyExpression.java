package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterPrefixNotDefinedException;
import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterSyntacticException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.structures.ObjectPropertyExpression;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.notNullValue;

import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterParserException;

public class TestObjectPropertyExpression {

	@Test
	public void testNamedProperty() throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		Map<String, String> prefixToNamespacesMap = new HashMap<>();
		ObjectPropertyExpression ope = ManchesterSyntaxUtils.parseObjectPropertyExpression(
				"<http://test.it/prop1>", SimpleValueFactory.getInstance(), prefixToNamespacesMap);

		assertThat(ope, notNullValue());

		Map<String, String> namespaceToPrefixsMap = new LinkedHashMap<>();
		namespaceToPrefixsMap.put("http://test.it/", "anothertest");

		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, true),
				Matchers.equalTo("<http://test.it/prop1>"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, true),
				Matchers.equalTo("anothertest:prop1"));
	}

	@Test
	public void testQNamedProperty() throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		Map<String, String> prefixToNamespacesMap = new HashMap<>();
		prefixToNamespacesMap.put("test", "http://test.it/");
		ObjectPropertyExpression ope = ManchesterSyntaxUtils.parseObjectPropertyExpression("test:prop1",
				SimpleValueFactory.getInstance(), prefixToNamespacesMap);

		assertThat(ope, notNullValue());

		Map<String, String> namespaceToPrefixsMap = new LinkedHashMap<>();
		namespaceToPrefixsMap.put("http://test.it/", "anothertest");

		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, true),
				Matchers.equalTo("<http://test.it/prop1>"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, true),
				Matchers.equalTo("anothertest:prop1"));
	}
	
	@Test
	public void testNamedInverseProperty() throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		Map<String, String> prefixToNamespacesMap = new HashMap<>();
		ObjectPropertyExpression ope = ManchesterSyntaxUtils.parseObjectPropertyExpression(
				"inverse <http://test.it/prop1>", SimpleValueFactory.getInstance(), prefixToNamespacesMap);

		assertThat(ope, notNullValue());

		Map<String, String> namespaceToPrefixsMap = new LinkedHashMap<>();
		namespaceToPrefixsMap.put("http://test.it/", "anothertest");

		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, true),
				Matchers.equalTo("INVERSE <http://test.it/prop1>"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, false),
				Matchers.equalTo("inverse <http://test.it/prop1>"));

		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, true),
				Matchers.equalTo("INVERSE anothertest:prop1"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, false),
				Matchers.equalTo("inverse anothertest:prop1"));
	}

	@Test
	public void testQNamedInverseProperty() throws ManchesterParserException, ManchesterSyntacticException, ManchesterPrefixNotDefinedException {
		Map<String, String> prefixToNamespacesMap = new HashMap<>();
		prefixToNamespacesMap.put("test", "http://test.it/");
		ObjectPropertyExpression ope = ManchesterSyntaxUtils.parseObjectPropertyExpression("INVERSE test:prop1",
				SimpleValueFactory.getInstance(), prefixToNamespacesMap);

		assertThat(ope, notNullValue());

		Map<String, String> namespaceToPrefixsMap = new LinkedHashMap<>();
		namespaceToPrefixsMap.put("http://test.it/", "anothertest");

		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, true),
				Matchers.equalTo("INVERSE <http://test.it/prop1>"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, false, false),
				Matchers.equalTo("inverse <http://test.it/prop1>"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, true),
				Matchers.equalTo("INVERSE anothertest:prop1"));
		assertThat(ope.getManchExpr(namespaceToPrefixsMap, true, false),
				Matchers.equalTo("inverse anothertest:prop1"));
	}


}
