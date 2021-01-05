package it.uniroma2.art.semanticturkey.properties.json.schema;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class JsonSchemaConverterTest {

	protected ObjectMapper objectMapper;

	@Before
	public void init() {
		objectMapper = buildObjectMapper();
	}

	protected ObjectMapper buildObjectMapper() {
		return RequestMappingHandlerAdapterPostProcessor.createObjectMapper();
	}

	public static class ExpectedProperties1 implements STProperties {
		@Override
		public String getShortName() {
			return "TestProp";
		}

		@STProperty(displayName = "boolean property", description = "This is a boolean property")
		public Boolean booleanProp;

		@STProperty(displayName = "string property", description = "This is a string property")
		@Size(min = 1, max = 5)
		@Pattern(regexp = "[\\w\\s]+")
		@Required
		public String stringProp = "hello world";

		@STProperty(displayName = "integer property", description = "This is an integer property")
		@Min(1)
		@Max(5)
		public Integer integerProp;

		@STProperty(displayName = "object property", description = "this is an object property")
		public ObjectNode objectProp;

		@STProperty(displayName = "set property", description = "this is a set property")
		public Set<String> setProp;

		@STProperty(displayName = "list property", description = "this is a list property")
		public List<String> listProp;

		@STProperty(displayName = "menu property", description = "this is a menu property")
		@Enumeration({ "A", "B" })
		public String menuProp;

	};

	@Test
	public void testSchema1Conversion() throws JSONException, ConversionException, JsonProcessingException {

		JsonSchemaConverter schemaConverter = new JsonSchemaConverter();
		RuntimeSTProperties actualProps = schemaConverter.convert(new InputStreamReader(
				this.getClass().getResourceAsStream("schema1.json"), StandardCharsets.UTF_8));

		JsonNode actualTree = objectMapper.valueToTree(actualProps);

		JsonNode expectedTree = objectMapper.valueToTree(new ExpectedProperties1());
		((ObjectNode) expectedTree).put("@type", RuntimeSTProperties.class.getName());

		JSONAssert.assertEquals(expectedTree.toString(), actualTree.toString(), true);
	}
}
