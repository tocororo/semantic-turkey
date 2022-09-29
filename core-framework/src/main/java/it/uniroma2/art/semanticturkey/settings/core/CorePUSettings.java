package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;
import java.util.Map;

public class CorePUSettings implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.CorePUSettings";

		public static final String shortName = keyBase + ".shortName";
		public static final String editingLanguage$description = keyBase + ".editingLanguage.description";
		public static final String editingLanguage$displayName = keyBase + ".editingLanguage.displayName";
        public static final String filterValueLanguages$description = keyBase + ".filterValueLanguages.description";
        public static final String filterValueLanguages$displayName = keyBase + ".filterValueLanguages.displayName";
        public static final String activeSchemes$description = keyBase + ".activeSchemes.description";
        public static final String activeSchemes$displayName = keyBase + ".activeSchemes.displayName";
        public static final String activeLexicon$description = keyBase + ".activeLexicon.description";
        public static final String activeLexicon$displayName = keyBase + ".activeLexicon.displayName";
        public static final String showFlags$description = keyBase + ".showFlags.description";
        public static final String showFlags$displayName = keyBase + ".showFlags.displayName";
		public static final String projectTheme$description = keyBase + ".projectTheme.description";
		public static final String projectTheme$displayName = keyBase + ".projectTheme.displayName";
		public static final String classTree$description = keyBase + ".classTree.description";
		public static final String classTree$displayName = keyBase + ".classTree.displayName";
		public static final String structurePanelFilter$description = keyBase + ".structurePanelFilter.description";
		public static final String structurePanelFilter$displayName = keyBase + ".structurePanelFilter.displayName";
		public static final String instanceList$description = keyBase + ".instanceList.description";
		public static final String instanceList$displayName = keyBase + ".instanceList.displayName";
		public static final String conceptTree$description = keyBase + ".conceptTree.description";
		public static final String conceptTree$displayName = keyBase + ".conceptTree.displayName";
		public static final String lexEntryList$description = keyBase + ".lexEntryList.description";
		public static final String lexEntryList$displayName = keyBase + ".lexEntryList.displayName";
		public static final String customTree$description = keyBase + ".customTree.description";
		public static final String customTree$displayName = keyBase + ".customTree.displayName";
		public static final String graphViewPartitionFilter$description = keyBase + ".graphViewPartitionFilter.description";
		public static final String graphViewPartitionFilter$displayName = keyBase + ".graphViewPartitionFilter.displayName";
		public static final String resourceView$description = keyBase + ".resourceView.description";
		public static final String resourceView$displayName = keyBase + ".resourceView.displayName";
		public static final String hideLiteralGraphNodes$description = keyBase + ".hideLiteralGraphNodes.description";
		public static final String hideLiteralGraphNodes$displayName = keyBase + ".hideLiteralGraphNodes.displayName";
		public static final String searchSettings$description = keyBase + ".searchSettings.description";
		public static final String searchSettings$displayName = keyBase + ".searchSettings.displayName";
		public static final String notificationsStatus$description = keyBase + ".notificationsStatus.description";
		public static final String notificationsStatus$displayName = keyBase + ".notificationsStatus.displayName";
		public static final String sheet2rdfSettings$description = keyBase + ".sheet2rdfSettings.description";
		public static final String sheet2rdfSettings$displayName = keyBase + ".sheet2rdfSettings.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.editingLanguage$description
			+ "}", displayName = "{" + MessageKeys.editingLanguage$displayName + "}")
	public String editingLanguage;

    @STProperty(description = "{" + MessageKeys.filterValueLanguages$description
            + "}", displayName = "{" + MessageKeys.filterValueLanguages$displayName + "}")
    public ValueFilterLanguages filterValueLanguages;

    @STProperty(description = "{" + MessageKeys.activeSchemes$description
            + "}", displayName = "{" + MessageKeys.activeSchemes$displayName + "}")
    public List<IRI> activeSchemes;

    @STProperty(description = "{" + MessageKeys.activeLexicon$description
            + "}", displayName = "{" + MessageKeys.activeLexicon$displayName + "}")
    public IRI activeLexicon;

    @STProperty(description = "{" + MessageKeys.showFlags$description
            + "}", displayName = "{" + MessageKeys.showFlags$displayName + "}")
    public Boolean showFlags;

	@STProperty(description = "{" + MessageKeys.projectTheme$description
			+ "}", displayName = "{" + MessageKeys.projectTheme$displayName + "}")
	public String projectTheme; // TODO: is this right?

	@STProperty(description = "{" + MessageKeys.structurePanelFilter$description
			+ "}", displayName = "{" + MessageKeys.structurePanelFilter$displayName + "}")
	public List<RDFResourceRole> structurePanelFilter;

	@STProperty(description = "{" + MessageKeys.classTree$description
			+ "}", displayName = "{" + MessageKeys.classTree$displayName + "}")
	public ClassTreePreferences classTree;

	@STProperty(description = "{" + MessageKeys.instanceList$description
			+ "}", displayName = "{" + MessageKeys.instanceList$displayName + "}")
	public InstanceListPreferences instanceList;

	@STProperty(description = "{" + MessageKeys.conceptTree$description
			+ "}", displayName = "{" + MessageKeys.conceptTree$displayName + "}")
	public ConceptTreePreferences conceptTree;

	@STProperty(description = "{" + MessageKeys.lexEntryList$description
			+ "}", displayName = "{" + MessageKeys.lexEntryList$displayName + "}")
	public LexEntryListPreferences lexEntryList;

	@STProperty(description = "{" + MessageKeys.customTree$description
			+ "}", displayName = "{" + MessageKeys.customTree$displayName + "}")
	public CustomTreeSettings customTree;

	@STProperty(description = "{" + MessageKeys.resourceView$description
			+ "}", displayName = "{" + MessageKeys.resourceView$displayName + "}")
	public ResourceViewPreferences resourceView;

	@STProperty(description = "{" + MessageKeys.graphViewPartitionFilter$description
			+ "}", displayName = "{" + MessageKeys.graphViewPartitionFilter$displayName + "}")
	public Map<String, List<String>> graphViewPartitionFilter;

	@STProperty(description = "{" + MessageKeys.hideLiteralGraphNodes$description
			+ "}", displayName = "{" + MessageKeys.hideLiteralGraphNodes$displayName + "}")
	public Boolean hideLiteralGraphNodes;

	@STProperty(description = "{" + MessageKeys.searchSettings$description
			+ "}", displayName = "{" + MessageKeys.searchSettings$displayName + "}")
	public SearchSettings searchSettings;

	@STProperty(description = "{" + MessageKeys.notificationsStatus$description
			+ "}", displayName = "{" + MessageKeys.notificationsStatus$displayName + "}")
	@Enumeration({"no_notifications", "in_app_only", "email_instant", "email_daily_digest"})
	public String notificationsStatus;

	@STProperty(description = "{" + MessageKeys.sheet2rdfSettings$description
			+ "}", displayName = "{" + MessageKeys.sheet2rdfSettings$displayName + "}")
	public Sheet2RdfSettings sheet2rdfSettings;

}
