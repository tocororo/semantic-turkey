package it.uniroma2.art.semanticturkey.project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacets;

import java.util.Map;

public class ProjectInfo {

    private final String name;
    private final boolean open;
    private final String baseURI;
    private final String defaultNamespace;
    private final String model;
    private final String lexicalizationModel;
    private final boolean historyEnabled;
    private final boolean validationEnabled;
    private final boolean blacklistingEnabled;
    private final boolean shaclEnabled;
    private final boolean undoEnabled;
    private final boolean readOnly;
    private final AccessResponse accessible;
    private final RepositoryLocation repositoryLocation;
    private final ProjectStatus status;
    private final Map<String, String> labels;
    private final String description;
    private final String createdAt;
    private final ProjectFacets facets;
    private final boolean openAtStartup;

    @JsonCreator
    public ProjectInfo(@JsonProperty("name") String name, @JsonProperty("open") boolean open,
            @JsonProperty("baseURI") String baseURI,
            @JsonProperty("defaultNamespace") String defaultNamespace, @JsonProperty("model") String model,
            @JsonProperty("lexicalizationModel") String lexicalizationModel,
            @JsonProperty("historyEnabled") boolean historyEnabled,
            @JsonProperty("validationEnabled") boolean validationEnabled,
            @JsonProperty("blacklistingEnabled") boolean blacklistingEnabled,
            @JsonProperty("shaclEnabled") boolean shaclEnabled,
            @JsonProperty("undoEnabled") boolean undoEnabled,
            @JsonProperty("readOnly") boolean readOnly,
            @JsonProperty("accessible") AccessResponse accessible,
            @JsonProperty("repositoryLocation") RepositoryLocation repositoryLocation,
            @JsonProperty("status") ProjectStatus status, @JsonProperty("labels") Map<String, String> labels,
            @JsonProperty("description") String description,
            @JsonProperty("createdAt") String createdAt, @JsonProperty("openAtStartup") boolean openAtStartup,
            ProjectFacets facets) {
        this.name = name;
        this.open = open;
        this.baseURI = baseURI;
        this.defaultNamespace = defaultNamespace;
        this.model = model;
        this.lexicalizationModel = lexicalizationModel;
        this.historyEnabled = historyEnabled;
        this.validationEnabled = validationEnabled;
        this.blacklistingEnabled = blacklistingEnabled;
        this.shaclEnabled = shaclEnabled;
        this.undoEnabled = undoEnabled;
        this.readOnly = readOnly;
        this.accessible = accessible;
        this.repositoryLocation = repositoryLocation;
        this.status = status;
        this.labels = labels;
        this.description = description;
        this.createdAt = createdAt;
        this.facets = facets;
        this.openAtStartup = openAtStartup;
    }

    public String getName() {
        return name;
    }

    public boolean isOpen() {
        return open;
    }

    public String getBaseURI() {
        return this.baseURI;
    }

    public String getDefaultNamespace() {
        return this.defaultNamespace;
    }

    public String getModel() {
        return model;
    }

    public String getLexicalizationModel() {
        return lexicalizationModel;
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public boolean isBlacklistingEnabled() {
        return blacklistingEnabled;
    }

    public boolean isUndoEnabled() {
        return undoEnabled;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isOpenAtStartup() {
        return openAtStartup;
    }


    @JsonProperty("shaclEnabled")
    public boolean isSHACLEnabled() {
        return shaclEnabled;
    }

    public AccessResponse getAccessible() {
        return accessible;
    }

    public RepositoryLocation getRepositoryLocation() {
        return repositoryLocation;
    }

    public ProjectStatus getStatus() {
        return this.status;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public ProjectFacets getFacets() {
        return facets;
    }
}
