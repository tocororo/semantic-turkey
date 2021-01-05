package it.uniroma2.art.semanticturkey.properties;

import java.util.List;

import javax.validation.constraints.Pattern;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.AnnotatedTypeBuilder;
import it.uniroma2.art.semanticturkey.properties.RuntimeSTProperties.PropertyDefinition;

public class RuntimeSTPropertiesTest {

	protected ObjectMapper objectMapper;

	@Before
	public void init() {
		objectMapper = buildObjectMapper();
	}

	protected ObjectMapper buildObjectMapper() {
		return RequestMappingHandlerAdapterPostProcessor.createObjectMapper();
	}

	@Test
	public void testProps() throws JSONException {
		STProperties expectedProps = new STProperties() {
			@Override
			public String getShortName() {
				return "TestProp";
			}

			@Override
			public String getHTMLDescription() {
				return "test description";
			}

			@Override
			public String getHTMLWarning() {
				return "test warning";
			}

			@STProperty(displayName = "string property", description = "this is a string property")
			@Required
			public String stringProp;

			@STProperty(displayName = "list property", description = "this is a list property")
			@Required
			public List<String> listProp;

			@STProperty(displayName = "menu property", description = "this is a menu property")
			@Enumeration({ "a", "b" })
			@Required
			public String menuProp;

			@STProperty(displayName = "patterned string property", description = "this is a patterned string property")
			@Pattern(regexp = "a*")
			@Required
			public String patternedString;

		};

		RuntimeSTProperties props = new RuntimeSTProperties("TestProp");
		props.setHtmlDescription("test description");
		props.setHtmlWarning("test warning");
		props.addProperty("stringProp", new PropertyDefinition("string property", "this is a string property",
				true, new AnnotatedTypeBuilder().withType(String.class).build()));
		props.addProperty("listProp",
				new PropertyDefinition("list property", "this is a list property", true,
						new AnnotatedTypeBuilder().withType(List.class)
								.withTypeArgument(new AnnotatedTypeBuilder().withType(String.class))
								.build()));
		PropertyDefinition menuPropDef = new PropertyDefinition("menu property", "this is a menu property",
				true, new AnnotatedTypeBuilder().withType(String.class).build());
		menuPropDef.setEnumeration("a", "b");
		props.addProperty("menuProp", menuPropDef);
		PropertyDefinition patternedStringDef = new PropertyDefinition("patterned string property", "this is a patterned string property",
				true, new AnnotatedTypeBuilder().withType(String.class).build());
		patternedStringDef.addAnnotation(Pattern.class, ImmutableMap.of("regexp", "a*"));
		props.addProperty("patternedString",
				patternedStringDef);

		JsonNode actualTree = objectMapper.valueToTree(props);

		JsonNode expectedTree = objectMapper.valueToTree(expectedProps);
		((ObjectNode) expectedTree).put("@type", RuntimeSTProperties.class.getName());
		
		JSONAssert.assertEquals(expectedTree.toString(), actualTree.toString(), true);
	}

}
