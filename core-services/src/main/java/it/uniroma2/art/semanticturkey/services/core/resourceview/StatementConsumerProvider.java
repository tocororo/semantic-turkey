package it.uniroma2.art.semanticturkey.services.core.resourceview;

import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.event.annotation.EventListener;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.BroadersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ClassAxiomsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ConstituentsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DatatypeDefinitionsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DenotationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EquivalentPropertyStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EvokedLexicalConcepts;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormBasedPreviewStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormRepresentationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.InSchemeStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LabelRelationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalFormsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalSensesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyChainStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyDisjointWithStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RDFSMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSNotesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSOrderedCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubtermsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TopConceptOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TypesStatementConsumer;
import it.uniroma2.art.semanticturkey.settings.core.CoreProjectSettings;
import it.uniroma2.art.semanticturkey.settings.core.ResourceViewCustomSectionSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.settings.events.SettingsDefaultsUpdated;
import it.uniroma2.art.semanticturkey.settings.events.SettingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class StatementConsumerProvider implements ApplicationListener<ApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StatementConsumerProvider.class);

    /**
     * Event handlers used to listens for project events, such as opening and closing
     */
    private ProjectManager.ProjectEventHandler prjEventHandler;

    private CustomFormManager customFormManager;
    private ProjectCustomViewsManager projCvManager;
    private SemanticTurkeyCoreSettingsManager coreSettingsManager;

    /**
     * Statements consumers provided out of the box by Semantic Turkey. The {@code key} is the section name.
     */
    private Map<String, StatementConsumer> factoryStatementConsumers;

    /**
     * Associates an (open) project with its resource view templates
     */
    private ConcurrentHashMap<String, Map<RDFResourceRole, List<StatementConsumer>>> project2templates;

    @Autowired
    ProjectCustomViewsManager projWidgetMgr;

    @Autowired
    public StatementConsumerProvider(CustomFormManager customFormManager, ProjectCustomViewsManager projCvManager,
                                     ObjectFactory<CODACoreProvider> codaProvider, SemanticTurkeyCoreSettingsManager coreSettingsManager) {
        this.projCvManager = projCvManager;
        this.coreSettingsManager = coreSettingsManager;

        factoryStatementConsumers = new LinkedHashMap<>();
        factoryStatementConsumers.put("types", new TypesStatementConsumer(projCvManager));
        factoryStatementConsumers.put("classaxioms", new ClassAxiomsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("datatypeDefinitions", new DatatypeDefinitionsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("lexicalizations", new LexicalizationsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("broaders", new BroadersStatementConsumer(projCvManager));
        factoryStatementConsumers.put("equivalentProperties", new EquivalentPropertyStatementConsumer(projCvManager));
        factoryStatementConsumers.put("disjointProperties", new PropertyDisjointWithStatementConsumer(projCvManager));
        factoryStatementConsumers.put("superproperties", new SubPropertyOfStatementConsumer(projCvManager));
        factoryStatementConsumers.put("subPropertyChains", new PropertyChainStatementConsumer(projCvManager));
        factoryStatementConsumers.put("facets", new PropertyFacetsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("domains", new DomainsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("ranges", new RangesStatementConsumer(projCvManager));
        factoryStatementConsumers.put("imports", new OntologyImportsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("properties", new OtherPropertiesStatementConsumer(projCvManager));
        factoryStatementConsumers.put("topconceptof", new TopConceptOfStatementConsumer(projCvManager));
        factoryStatementConsumers.put("schemes", new InSchemeStatementConsumer(projCvManager));
        factoryStatementConsumers.put("members", new SKOSCollectionMembersStatementConsumer(projCvManager));
        factoryStatementConsumers.put("membersOrdered", new SKOSOrderedCollectionMembersStatementConsumer(projCvManager));
        factoryStatementConsumers.put("labelRelations", new LabelRelationsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("notes", new SKOSNotesStatementConsumer(projCvManager));
        factoryStatementConsumers.put("lexicalForms", new LexicalFormsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("lexicalSenses", new LexicalSensesStatementConsumer(projCvManager));
        factoryStatementConsumers.put("denotations", new DenotationsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("evokedLexicalConcepts", new EvokedLexicalConcepts(projCvManager));
        factoryStatementConsumers.put("formBasedPreview", new FormBasedPreviewStatementConsumer(customFormManager, codaProvider));
        factoryStatementConsumers.put("subterms", new SubtermsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("constituents", new ConstituentsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("formRepresentations", new FormRepresentationsStatementConsumer(projCvManager));
        factoryStatementConsumers.put("rdfsMembers", new RDFSMembersStatementConsumer(projCvManager));

        project2templates = new ConcurrentHashMap<>();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent
                || event instanceof ContextRefreshedEvent) {
            if (prjEventHandler != null) {
                ProjectManager.unregisterProjectEventHandler(prjEventHandler);
                prjEventHandler = null;
            }
        }

        if (event instanceof ContextStartedEvent || event instanceof ContextRefreshedEvent) {
            prjEventHandler = new ProjectManager.ProjectEventHandler() {

                @Override
                public void beforeProjectTearDown(Project project) {
                    unregisterProject(project);
                }

                @Override
                public void afterProjectInitialization(Project project) {
                    registerProject(project);
                }
            };
            ProjectManager.registerProjectEventHandler(prjEventHandler);
        }
    }

    private void registerProject(Project project) {
        try {
            Map<RDFResourceRole, List<StatementConsumer>> projectTemplates = computeTemplatesForProject(project);
            project2templates.put(project.getName(), projectTemplates);
        } catch (Exception e) {
            logger.error("Unable to register the newly opened project: " + project.getName(), e);
        }
    }

    private Map<RDFResourceRole, List<StatementConsumer>> computeTemplatesForProject(Project project) throws it.uniroma2.art.semanticturkey.properties.STPropertyAccessException {
        CoreProjectSettings coreProjectSettings = coreSettingsManager.getProjectSettings(project);

        Map<String, ResourceViewCustomSectionSettings> customSectionsSettings = Optional.ofNullable(coreProjectSettings.resourceView).map(s -> s.customSections).orElse(Collections.emptyMap());
        Map<RDFResourceRole, List<String>> templatesSettings = Optional.ofNullable(coreProjectSettings.resourceView).map(s -> s.templates).orElse(Collections.emptyMap());

        Map<String, StatementConsumer> customSections = customSectionsSettings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new AbstractPropertyMatchingStatementConsumer(projCvManager, entry.getKey(), entry.getValue().matchedProperties)));

        Map<RDFResourceRole, List<StatementConsumer>> projectTemplates = new HashMap<>();

        for (Map.Entry<RDFResourceRole, List<String>> entry : templatesSettings.entrySet()) {
            RDFResourceRole resourceRole = entry.getKey();
            List<String> roleTemplateSections = entry.getValue();

            // resolve sections names against factory ones and then against custom ones. Skip invalid sections

            List<StatementConsumer> roleTemplate = roleTemplateSections.stream().map(s -> factoryStatementConsumers.getOrDefault(s, customSections.get(s))).filter(Objects::nonNull).collect(Collectors.toList());

            projectTemplates.put(resourceRole, roleTemplate);
        }
        return projectTemplates;
    }

    private void unregisterProject(Project project) {
        project2templates.remove(project.getName());
    }

    @EventListener
    public void onSettingsUpdated(SettingsEvent event) {
        // skips events not related to the core settings
        if (!Objects.equals(event.getSettingsManager().getId(), SemanticTurkeyCoreSettingsManager.class.getName())) return;

        // skips events not related to the project settings (or their defaults)
        if (!Objects.equals(event.getScope(), Scope.PROJECT)) return;

        // defaults updated, then clear all templates to be sure
        if (event instanceof SettingsDefaultsUpdated) {
            project2templates.clear();
        } else {
            // otherwise, we are setting a project template, so just clear the associated template
            project2templates.remove(event.getProject().getName());
        }
    }

    public List<StatementConsumer> getTemplateForResourceRole(Project project, RDFResourceRole role) {
        // there should always be an entry of (open) project

        @Nullable
        Map<RDFResourceRole, List<StatementConsumer>> role2template = project2templates.computeIfAbsent(project.getName(), projectName -> {
            try {
                return computeTemplatesForProject(project);
            } catch (STPropertyAccessException e) {
                logger.error("Unable to build the template for the project project: " + project.getName(), e);
                return Collections.emptyMap();
            }
        });

        List<StatementConsumer> template = null;

        if (role.isProperty()) {
            if (RDFResourceRole.subsumes(RDFResourceRole.objectProperty, role)) {
                template = role2template.get(RDFResourceRole.objectProperty);
            }

            if (template == null && RDFResourceRole.subsumes(RDFResourceRole.datatypeProperty, role)) {
                template = role2template.get(RDFResourceRole.datatypeProperty);
            }
            if (template == null && RDFResourceRole.subsumes(RDFResourceRole.ontologyProperty, role)) {
                template = role2template.get(RDFResourceRole.ontologyProperty);
            }
            if (template == null && RDFResourceRole.subsumes(RDFResourceRole.annotationProperty, role)) {
                template = role2template.get(RDFResourceRole.annotationProperty);
            }
            if (template == null)  {
                template = role2template.get(RDFResourceRole.property);
            }
        } else {
            template = role2template.get(role);
        }

        if (template != null) {
            return template;
        } else {
            // if no template has been found, fallback to the one for individuals. If this is not defined (e.g. corrupted settings), fallback to a simple properties listing
            return role2template.getOrDefault(RDFResourceRole.individual, Arrays.asList(factoryStatementConsumers.get("properties")));
        }
    }

}
