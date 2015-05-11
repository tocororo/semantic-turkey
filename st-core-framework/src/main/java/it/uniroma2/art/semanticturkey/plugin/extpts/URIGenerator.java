package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.semanticturkey.data.id.ARTURIResAndRandomString;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.util.Map;

public interface URIGenerator {
	

	/**
	 * This method generates a URI base on the template and the valueMapping provided.<br/>
	 * The valueMapping must contain key-value pairs that associate the placeholder used in the template with
	 * the values to assign to them
	 * 
	 * @param template
	 *            a regex expression with optional (defineable) placeholders contained between ${ and }. The
	 *            only mandatory (and provided by default) placeholder is rand(), which generates a random
	 *            code for the URI local name. rand() can have an optional single argument
	 *            <code>RandCode</code>.<br/>
	 *            RandCode can be one of:
	 *            <ul>
	 *            <li>DATETIMEMS: uses the current time in MS for generating the ID</li>
	 *            <li>UUID: generates a random UUID</li>
	 *            <li>TRUNCUUID4: generates a random UUID and then truncates up to the first 4 chars</li>
	 *            <li>TRUNCUUID8: generates a random UUID and then truncates up to the first 8 chars (first
	 *            section of the UUID before the hyphen)</li>
	 *            <li>TRUNCUUID12: generates a random UUID and then truncates up to the first 12 chars
	 *            (including the hyphen)</li>
	 *            </ul>
	 *            If this argument is not provided explcitly between the round brackets of rand(), it is
	 *            looked up on the project property <code>uriRndCodeGenerator</code>.<br/>
	 *            If that property is not found in the project, then a default is assumed (TRUNCUUID8).<br/>
	 *            e.g. <code>c_${rand(TRUNCUUID4)}</code> will generate resource localnames such as c_47d3
	 * @param valueMapping
	 *            can be used to define new placeholders, by associating them to values computed outside of
	 *            the regexp. For instance, in the case of SKOSXL labels, one might want to add a lang placeholder filled with the value of the literalform of the xlabel<br/>
	 *            e.g <code>xl_${lang}_${rand()}</code> will generate skosxl labels such as: xl_en_4f56ed21
	 * @return
	 * @throws URIGenerationException
	 */
	ARTURIResAndRandomString generateURI(STServiceContext stServiceContext, String template, Map<String, String> valueMapping) throws URIGenerationException;
}
