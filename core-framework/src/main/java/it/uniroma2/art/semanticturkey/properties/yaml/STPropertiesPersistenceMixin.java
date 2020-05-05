package it.uniroma2.art.semanticturkey.properties.yaml;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A mix-in specifying serialization/deserialization instructions for STProperties.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@JsonSerialize(using = STPropertiesPersistenceSerializer.class)
public class STPropertiesPersistenceMixin {

}
