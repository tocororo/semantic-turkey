package it.uniroma2.art.semanticturkey.settings.events;

import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;

public class SettingsEvent extends Event {
    private final Project project;
    private final STUser user;
    private final UsersGroup group;
    private final Scope scope;
    private final Settings settings;

    public SettingsEvent(SettingsManager settingsManager, Project project, STUser user, UsersGroup group, Scope scope, Settings settings) {
        super(settingsManager);
        this.project = project;
        this.user = user;
        this.group = group;
        this.scope = scope;
        this.settings = settings;
    }

    public SettingsManager getSettingsManager() {
        return (SettingsManager) getSource();
    }

    public Project getProject() {
        return project;
    }

    public STUser getUser() {
        return user;
    }

    public UsersGroup getGroup() {
        return group;
    }

    public Scope getScope() {
        return scope;
    }

    public Settings getSettings() {
        return settings;
    }

}
