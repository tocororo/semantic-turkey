package it.uniroma2.art.semanticturkey.config.customview;

import it.uniroma2.art.semanticturkey.customviews.CustomViewModelEnum;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.semanticturkey.vocabulary.RDFTypesEnum;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class AdvSingleValueView extends CustomView {

    public enum ValueSelectionMode {
        none, //no update in widget
        inline, //inline NT editing
        picker //value picker
    }

    @Override
    public CustomViewModelEnum getModelType() {
        return CustomViewModelEnum.adv_single_value;
    }

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customview.AbstractSparqlBasedCustomView";

        public static final String shortName = keyBase + ".shortName";
        public static final String retrieve$description = keyBase + ".retrieve.description";
        public static final String retrieve$displayName = keyBase + ".retrieve.displayName";
        public static final String update$description = keyBase + ".update.description";
        public static final String update$displayName = keyBase + ".update.displayName";
        public static final String updateMode$description = keyBase + ".updateMode.description";
        public static final String updateMode$displayName = keyBase + ".updateMode.displayName";
        public static final String valueType$description = keyBase + ".valueType.description";
        public static final String valueType$displayName = keyBase + ".valueType.displayName";
        public static final String datatype$description = keyBase + ".datatype.description";
        public static final String datatype$displayName = keyBase + ".datatype.displayName";
        public static final String classes$description = keyBase + ".classes.description";
        public static final String classes$displayName = keyBase + ".classes.displayName";
    }

    @Required
    @STProperty(description = "{" + MessageKeys.retrieve$description + "}", displayName = "{" + MessageKeys.retrieve$displayName + "}")
    public String retrieve;

    /**
     * tells if and how the new value can be specified (inline|picker)
     */
    @Required
    @STProperty(description = "{" + MessageKeys.updateMode$description + "}", displayName = "{" + MessageKeys.updateMode$displayName + "}")
    public ValueSelectionMode updateMode;

    /**
     * when updateMode is not "none", indicates the query to use for updating value
     */
    @STProperty(description = "{" + MessageKeys.update$description + "}", displayName = "{" + MessageKeys.update$displayName + "}")
    public String update;

    /**
     * when updateMode is picker, tells what kind of value the picker should allow (handled only resource|literal)
     */
    @STProperty(description = "{" + MessageKeys.valueType$description + "}", displayName = "{" + MessageKeys.valueType$displayName + "}")
    public RDFTypesEnum valueType;

    /**
     * when valueType is literal, restricts the value selection to a given datatype
     */
    @STProperty(description = "{" + MessageKeys.datatype$description + "}", displayName = "{" + MessageKeys.datatype$displayName + "}")
    public IRI datatype;

    /**
     * when valueType is resource, restricts the value selection to the given classes instances
     */
    @STProperty(description = "{" + MessageKeys.classes$description + "}", displayName = "{" + MessageKeys.classes$displayName + "}")
    public List<IRI> classes;
}
