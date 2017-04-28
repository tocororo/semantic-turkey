package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Armando Stellato
 * 
 *         can be used to specify the type of property content. These types can drive consistency checks or
 *         the painting of UI for property setting. Expected values are:<br/>
 *         <ul>
 *         <li>url</li>
 *         <li>file</li>
 *         <li>directory</li>
 *         <li>boolean (<em>value must be <code>true</code> or <code>false</code>, lowercase)</em></li>
 *         </ul>
 *         see also {@link ContentTypeVocabulary}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentType {
	String value();
}
