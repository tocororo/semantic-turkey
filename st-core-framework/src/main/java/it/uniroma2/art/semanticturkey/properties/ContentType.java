package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Armando Stellato
 * 
 *         can be used to specify the type of property content. Expected values are:<br/>
 *         <ul>
 *         <li>url</li>
 *         <li>file</li>
 *         <li>directory</li>
 *         </ul>
 *         the type is expressed by a String as new content types can be dynamically added. See also:
 *         {@link ContentTypeVocabulary}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentType {
	String value();
}
