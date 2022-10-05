package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class CustomTreeSettings implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.CustomTreeSettings";

        public static final String shortName = keyBase + ".shortName";
        public static final String enabled$description = keyBase + ".enabled.description";
        public static final String enabled$displayName = keyBase + ".enabled.displayName";
        public static final String rootSelection$description = keyBase + ".rootSelection.description";
        public static final String rootSelection$displayName = keyBase + ".rootSelection.displayName";
        public static final String roots$description = keyBase + ".roots.description";
        public static final String roots$displayName = keyBase + ".roots.displayName";
        public static final String type$description = keyBase + ".type.description";
        public static final String type$displayName = keyBase + ".type.displayName";
        public static final String includeSubtype$description = keyBase + ".includeSubtype.description";
        public static final String includeSubtype$displayName = keyBase + ".includeSubtype.displayName";
        public static final String hierarchicalProperty$description = keyBase + ".hierarchicalProperty.description";
        public static final String hierarchicalProperty$displayName = keyBase + ".hierarchicalProperty.displayName";
        public static final String inverseHierarchyDirection$description = keyBase + ".inverseHierarchyDirection.description";
        public static final String inverseHierarchyDirection$displayName = keyBase + ".inverseHierarchyDirection.displayName";
        public static final String includeSubProp$description = keyBase + ".includeSubProp.description";
        public static final String includeSubProp$displayName = keyBase + ".includeSubProp.displayName";
    }

    public static final String ROOT_SELECTION_ALL = "all"; //roots are all resources of the given type without parent
    public static final String ROOT_SELECTION_WITH_CHILD_ONLY = "with_child_only"; //all resources of the given type without parent and with children
    public static final String ROOT_SELECTION_ENUMERATION = "enumeration"; //resources explicitly provided in roots setting

    @Override
    public String getShortName() {
        return "{" + CustomTreeSettings.MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.enabled$description+ "}", displayName = "{" + MessageKeys.enabled$displayName + "}")
    public Boolean enabled = false;

    @STProperty(description = "{" + MessageKeys.rootSelection$description+ "}", displayName = "{" + MessageKeys.rootSelection$displayName + "}")
    @Enumeration({ROOT_SELECTION_ALL, ROOT_SELECTION_WITH_CHILD_ONLY, ROOT_SELECTION_ENUMERATION})
    @Required
    public String rootSelection;

    @STProperty(description = "{" + MessageKeys.roots$description+ "}", displayName = "{" + MessageKeys.roots$displayName + "}")
    public List<IRI> roots;


    @STProperty(description = "{" + MessageKeys.type$description+ "}", displayName = "{" + MessageKeys.type$displayName + "}")
    public IRI type;

    @STProperty(description = "{" + MessageKeys.includeSubtype$description+ "}", displayName = "{" + MessageKeys.includeSubtype$displayName + "}")
    public Boolean includeSubtype = false;

    @STProperty(description = "{" + MessageKeys.hierarchicalProperty$description+ "}", displayName = "{" + MessageKeys.hierarchicalProperty$displayName + "}")
    @Required
    public IRI hierarchicalProperty; //from parent to child (unless specified by inverseHierarchyDirection)

    @STProperty(description = "{" + MessageKeys.inverseHierarchyDirection$description+ "}", displayName = "{" + MessageKeys.inverseHierarchyDirection$displayName + "}")
    public Boolean inverseHierarchyDirection = false; //if true, the direction is from child to parent (e.g. skos:broader)

    @STProperty(description = "{" + MessageKeys.includeSubProp$description+ "}", displayName = "{" + MessageKeys.includeSubProp$displayName + "}")
    public Boolean includeSubProp = false;
    
}
