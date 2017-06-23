package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * Describes a set of properties in a declarative way.
 * 
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 */
public interface STProperties {

	/**
	 * returns all the properties of the class implementing this interface, which have been annotated
	 * as {@link STProperty}
	 * 
	 * @return
	 */
	public abstract Collection<String> getProperties();

	/**
	 * gets the value of a property previously set on this set of properties
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	public abstract Object getPropertyValue(String id) throws PropertyNotFoundException;

	/**
	 * sets the value of the {@link STProperty} to <code>value</code><br/>
	 * Note that it is possible to pass the value both with the proper type, if it is known in advance, as
	 * well with a generic String, which will be evaluated and converted to the appropriate type. Supported
	 * types are now:
	 * <ul>
	 * <li>Boolean</li>
	 * <li>Integer</li>
	 * <li>Long</li>
	 * <li>Double</li>
	 * </ul>
	 * 
	 * @param id
	 * @param value
	 * @throws WrongPropertiesException
	 */
	public abstract void setPropertyValue(String id, Object value) throws WrongPropertiesException;

	/**
	 * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
	 * <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws WrongPropertiesException
	 * @throws IOException
	 */
	public abstract void loadProperties(File propertyFile) throws WrongPropertiesException, IOException;

	/**
	 * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
	 * <code>properties</code>
	 * 
	 * @param properties
	 * @throws WrongPropertiesException
	 */
	public abstract void setProperties(Properties properties) throws WrongPropertiesException;

	/**
	 * stores the properties in this instance in file <code>propertyFile</code>
	 * 
	 * @param propertyFile
	 * @throws IOException
	 * @throws WrongPropertiesException
	 */
	public abstract void storeProperties(File propertyFile) throws IOException, WrongPropertiesException;

	/**
	 * stores the properties in this instance in the given {@link Properties} object <code>properties</code>
	 * 
	 * @param propertyFile
	 * @throws IOException
	 * @throws WrongPropertiesException
	 */
	public abstract void storeProperties(Properties properties) throws IOException, WrongPropertiesException;

	/**
	 * get the expected type of content for the property. Can be used by external tools to drive the
	 * acquisition of that value or to check
	 * 
	 * @param parID
	 * @return
	 * @throws PropertyNotFoundException
	 */
	public abstract String getPropertyContentType(String parID) throws PropertyNotFoundException;

	/**
	 * tells if this set of properties needs to be explicitly set by the user or if it can be used in its
	 * default settings
	 * 
	 * @return
	 */
	public abstract boolean hasRequiredProperties();

	public abstract boolean isRequiredProperty(String parID) throws PropertyNotFoundException;

	/**
	 * this method returns the description of a property
	 * 
	 * @param id
	 * @return
	 * @throws PropertyNotFoundException
	 */
	public abstract String getPropertyDescription(String id) throws PropertyNotFoundException;

	/**
	 * @return a humanly understandable short name representing the type of this property set
	 */
	public abstract String getShortName();

}